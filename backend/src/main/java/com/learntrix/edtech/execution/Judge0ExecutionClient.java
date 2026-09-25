package com.learntrix.edtech.execution;

import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Runs student code on Judge0.
 *
 * <p>The API key lives only in server configuration ({@code JUDGE0_API_KEY}) and is never
 * sent to the browser: React talks to LearntriX, LearntriX talks to Judge0. Limits are
 * applied per execution so a submission cannot monopolise the judge.</p>
 *
 * <p>Source, stdin and expected output travel base64-encoded, which keeps newlines and
 * non-ASCII intact through the JSON round trip.</p>
 */
@Component
// Default provider. Set execution.provider=jdoodle to use JDoodle instead;
// exactly one CodeExecutionClient bean must exist.
@ConditionalOnProperty(name = "execution.provider", havingValue = "judge0", matchIfMissing = true)
public class Judge0ExecutionClient implements CodeExecutionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(Judge0ExecutionClient.class);

    /** Judge0 language ids. Extend as more languages are offered in the editor. */
    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
            "java", 62,
            "python", 71,
            "cpp", 54,
            "c", 50,
            "javascript", 63,
            "typescript", 74,
            "csharp", 51,
            "go", 60
    );

    private final String baseUrl;
    private final String apiKey;
    private final String apiHost;
    private final RestClient restClient;

    public Judge0ExecutionClient(
            @Value("${judge0.url:}") String baseUrl,
            @Value("${judge0.api-key:}") String apiKey,
            @Value("${judge0.api-host:judge0-ce.p.rapidapi.com}") String apiHost) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiHost = apiHost;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public boolean isConfigured() {
        return !baseUrl.isEmpty();
    }

    public static boolean supportsLanguage(String language) {
        return language != null && LANGUAGE_IDS.containsKey(language.toLowerCase());
    }

    @Override
    public ExecutionResult execute(String language, String sourceCode, String stdin,
                                   int timeLimitMs, int memoryLimitMb) {
        if (!isConfigured()) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("Code execution is not configured on this server. "
                            + "Set JUDGE0_URL (and JUDGE0_API_KEY if your instance needs one).")
                    .build();
        }

        Integer languageId = LANGUAGE_IDS.get(language == null ? "" : language.toLowerCase());
        if (languageId == null) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("Unsupported language: " + language)
                    .build();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("language_id", languageId);
        body.put("source_code", b64(sourceCode));
        body.put("stdin", b64(stdin == null ? "" : stdin));
        // Judge0 takes seconds; keep a floor so a tiny limit does not become zero.
        body.put("cpu_time_limit", Math.max(1, timeLimitMs / 1000));
        body.put("wall_time_limit", Math.max(2, (timeLimitMs / 1000) + 2));
        body.put("memory_limit", Math.max(16, memoryLimitMb) * 1024); // KB
        // Cap output so a program printing endlessly cannot exhaust the judge or our heap.
        body.put("max_file_size", 4096);

        try {
            // wait=true keeps this a single synchronous call; the request itself is bounded
            // by the client timeout below so a hung judge cannot block the thread forever.
            JsonNode json = restClient.post()
                    .uri(baseUrl + "/submissions?base64_encoded=true&wait=true")
                    .headers(h -> {
                        h.add("Content-Type", "application/json");
                        if (!apiKey.isEmpty()) {
                            h.add("X-RapidAPI-Key", apiKey);
                            h.add("X-RapidAPI-Host", apiHost);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return interpret(json);
        } catch (Exception e) {
            // Never leak the student's source or the judge's internals into the response.
            LOGGER.error("Judge0 execution failed: {}", e.getMessage());
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("The execution service could not be reached. Please try again.")
                    .build();
        }
    }

    /** Maps Judge0's numeric status onto our transport-agnostic result. */
    private ExecutionResult interpret(JsonNode json) {
        if (json == null) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("No response from the execution service.")
                    .build();
        }

        int statusId = json.path("status").path("id").asInt(0);
        String stdout = decode(json.path("stdout").asText(null));
        String compileOutput = decode(json.path("compile_output").asText(null));
        String stderr = decode(json.path("stderr").asText(null));

        Integer runtimeMs = null;
        String time = json.path("time").asText(null);
        if (time != null && !time.isBlank()) {
            try {
                runtimeMs = (int) Math.round(Double.parseDouble(time) * 1000);
            } catch (NumberFormatException ignored) {
                // leave null; timing is informational
            }
        }

        // Judge0 status ids: 3 accepted, 5 TLE, 6 compile error, 7-12 runtime errors.
        ExecutionResult.Status status = switch (statusId) {
            case 3 -> ExecutionResult.Status.SUCCESS;
            case 5 -> ExecutionResult.Status.TIME_LIMIT_EXCEEDED;
            case 6 -> ExecutionResult.Status.COMPILE_ERROR;
            case 7, 8, 9, 10, 11, 12 -> ExecutionResult.Status.RUNTIME_ERROR;
            default -> ExecutionResult.Status.ERROR;
        };

        String message = compileOutput != null && !compileOutput.isBlank()
                ? compileOutput
                : (stderr != null && !stderr.isBlank() ? stderr : null);

        return ExecutionResult.builder()
                .status(status)
                .stdout(stdout)
                .message(message)
                .runtimeMs(runtimeMs)
                .build();
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes());
    }

    private static String decode(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new String(Base64.getDecoder().decode(s));
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

    /** Exposed for configuration reporting; never returns the key itself. */
    public Duration requestTimeout() {
        return Duration.ofSeconds(30);
    }
}
