package com.learntrix.edtech.ai;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.learntrix.edtech.common.exception.BusinessException;

/**
 * Gemini, over its REST API.
 *
 * <p>Uses Spring's {@link RestClient} rather than a vendor SDK, for the same reason
 * {@code Judge0ExecutionClient} does: one external HTTP call does not justify a dependency,
 * and the two integrations then look alike.</p>
 *
 * <p><b>The API key never leaves this class.</b> It travels in a request header rather than
 * a query string so it cannot end up in an access log or a redirect, and every failure below
 * is translated into a message safe to show a teacher - upstream text is never echoed back,
 * because provider errors can quote the request.</p>
 */
@Component
public class GeminiAiService implements AiProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiAiService.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestClient restClient;

    public GeminiAiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3-flash-preview}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.timeout-seconds:90}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.baseUrl = baseUrl;

        // Generating ten questions is not a fast call, but it must still be bounded - an
        // unbounded read would pin a request thread for as long as the provider hangs.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public String generateJson(String systemInstruction, String userPrompt,
                               Map<String, Object> responseSchema) {
        if (!isConfigured()) {
            throw new BusinessException("AI_NOT_CONFIGURED",
                    "The AI assistant is not configured on this server. "
                            + "Set GEMINI_API_KEY and restart the backend.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt)))));
        // responseMimeType + responseSchema is what stops the model replying in prose or
        // wrapping the object in a markdown fence.
        body.put("generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", responseSchema,
                "temperature", 0.7));

        JsonNode json;
        try {
            json = restClient.post()
                    .uri(baseUrl + "/models/" + model + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw translate(res.getStatusCode().value());
                    })
                    .body(JsonNode.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Could be a timeout, DNS failure, TLS problem. The detail is useful in the log
            // and useless (or leaky) in the response.
            LOGGER.error("Gemini call failed: {}", e.getClass().getSimpleName());
            throw new BusinessException("AI_UNAVAILABLE",
                    "The AI service could not be reached. Please try again.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        return extractText(json);
    }

    /** Maps provider status codes onto messages a teacher can act on. */
    private BusinessException translate(int status) {
        // 400 usually means a malformed request, but an unparseable key lands here too.
        if (status == 400 || status == 401 || status == 403) {
            LOGGER.error("Gemini rejected the credential or request (HTTP {})", status);
            return new BusinessException("AI_AUTH_FAILED",
                    "The AI service rejected this server's credentials. "
                            + "Check that GEMINI_API_KEY is valid.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (status == 404) {
            LOGGER.error("Gemini model '{}' not found", model);
            return new BusinessException("AI_MODEL_NOT_FOUND",
                    "The configured AI model is not available. Check the gemini.model setting.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (status == 429) {
            return new BusinessException("AI_RATE_LIMITED",
                    "The AI service is rate limited right now. Please wait a moment and try again.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        // 503 is common on newly released models under load, and is genuinely retryable.
        LOGGER.warn("Gemini returned HTTP {}", status);
        return new BusinessException("AI_UNAVAILABLE",
                "The AI service is busy at the moment. Please try again in a few seconds.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    /** Digs the generated JSON text out of the candidate envelope. */
    private String extractText(JsonNode json) {
        if (json == null) {
            throw emptyResponse();
        }
        JsonNode parts = json.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            // A blocked or truncated generation lands here.
            String finish = json.path("candidates").path(0).path("finishReason").asText("");
            LOGGER.warn("Gemini returned no usable content (finishReason={})", finish);
            throw emptyResponse();
        }
        String text = parts.path(0).path("text").asText(null);
        if (text == null || text.isBlank()) {
            throw emptyResponse();
        }
        return text;
    }

    private BusinessException emptyResponse() {
        return new BusinessException("AI_EMPTY_RESPONSE",
                "The AI service returned nothing usable. Please try again, "
                        + "or rephrase your topic and instructions.",
                HttpStatus.BAD_GATEWAY);
    }
}
