package com.learntrix.edtech.execution;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Runs student code on JDoodle.
 *
 * <p>An alternative to {@link Judge0ExecutionClient}, selected with
 * {@code execution.provider=jdoodle}. Both satisfy {@link CodeExecutionClient}, so nothing
 * above this class changes when the provider does.</p>
 *
 * <p>JDoodle authenticates with a clientId/clientSecret pair sent in the request body rather
 * than a header. Both live only in server configuration and are never logged, never returned
 * in a response, and never sent to the browser.</p>
 *
 * <p><b>A real difference from Judge0:</b> JDoodle replies with a single {@code output}
 * string and no field distinguishing a compile error from a runtime error or a clean run.
 * Compiler diagnostics simply arrive as output. This class therefore reports most outcomes
 * as SUCCESS and lets the expected-output comparison decide pass or fail - which is correct
 * for grading, but means a student sees "wrong answer" where Judge0 would have said
 * "compile error". The compiler message is still shown to them.</p>
 */
@Component
@ConditionalOnProperty(name = "execution.provider", havingValue = "jdoodle")
public class JDoodleExecutionClient implements CodeExecutionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDoodleExecutionClient.class);

    /** Our language keys mapped to JDoodle's. */
    private static final Map<String, String> LANGUAGE_CODES = Map.of(
            "java", "java",
            "python", "python3",
            "cpp", "cpp17",
            "c", "c",
            "javascript", "nodejs",
            "typescript", "typescript",
            "csharp", "csharp",
            "go", "go"
    );

    /**
     * Newest version position per language, verified against the live API.
     *
     * <p>versionIndex is a <em>position</em> in JDoodle's version list, not a version
     * number, and each language has its own range - java accepts [0..6] while cpp17 only
     * accepts [0..3]. A single shared index is therefore wrong: index 6 runs Java 26 but
     * is rejected outright for C++.</p>
     *
     * <p>These positions shift when JDoodle adds a compiler, so each is overridable with
     * {@code jdoodle.version-index.<language>}. An unknown language falls back to "0",
     * which every language supports.</p>
     */
    private static final Map<String, String> DEFAULT_VERSION_INDEX = Map.of(
            "java", "6",        // 26.0.2
            "python", "6",      // 3.14.3
            "cpp", "3",         // max for cpp17
            "c", "7",
            "javascript", "7",  // nodejs
            "typescript", "1",
            "csharp", "6",
            "go", "6"
    );

    private final String clientId;
    private final String clientSecret;
    private final String baseUrl;
    private final Environment environment;
    private final RestClient restClient;

    public JDoodleExecutionClient(
            @Value("${jdoodle.client-id:}") String clientId,
            @Value("${jdoodle.client-secret:}") String clientSecret,
            @Value("${jdoodle.url:https://api.jdoodle.com/v1/execute}") String baseUrl,
            Environment environment) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.baseUrl = baseUrl;
        this.environment = environment;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean isConfigured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    @Override
    public ExecutionResult execute(String language, String sourceCode, String stdin,
                                   int timeLimitMs, int memoryLimitMb) {
        if (!isConfigured()) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("Code execution is not configured on this server. "
                            + "Set JDOODLE_CLIENT_ID and JDOODLE_CLIENT_SECRET.")
                    .build();
        }

        String code = LANGUAGE_CODES.get(language == null ? "" : language.toLowerCase());
        if (code == null) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("Unsupported language: " + language)
                    .build();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", clientId);
        body.put("clientSecret", clientSecret);
        body.put("script", sourceCode == null ? "" : sourceCode);
        body.put("stdin", stdin == null ? "" : stdin);
        body.put("language", code);
        body.put("versionIndex", versionIndexFor(language));

        try {
            JsonNode json = restClient.post()
                    .uri(baseUrl)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return interpret(json);
        } catch (Exception e) {
            // Never let the student's source or our credentials reach a log or a response.
            LOGGER.error("JDoodle execution failed: {}", e.getClass().getSimpleName());
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("The execution service could not be reached. Please try again.")
                    .build();
        }
    }


    /**
     * Config override wins, then the verified default, then the universally valid "0".
     *
     * <p>Looked up per call rather than bound once, so a language can be pinned with
     * {@code jdoodle.version-index.java: 5} without this class knowing the language set
     * in advance.</p>
     */
    private String versionIndexFor(String language) {
        String key = language == null ? "" : language.toLowerCase();
        String override = environment.getProperty("jdoodle.version-index." + key);
        if (override != null && !override.isBlank()) return override.trim();
        return DEFAULT_VERSION_INDEX.getOrDefault(key, "0");
    }

    /**
     * Maps a JDoodle reply onto our transport-agnostic result.
     *
     * <p>JDoodle has no status field for compile vs runtime failure, but it does leak the
     * distinction structurally: when compilation fails the program never runs, so
     * {@code cpuTime} and {@code memory} come back null while {@code output} holds the
     * compiler diagnostics. A successful or crashing run always reports timing. That is a
     * far steadier signal than grepping the output for the word "error", which would
     * misfire on any program that legitimately prints it.</p>
     */
    private ExecutionResult interpret(JsonNode json) {
        if (json == null) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("No response from the execution service.")
                    .build();
        }

        int statusCode = json.path("statusCode").asInt(200);
        String error = json.path("error").asText(null);

        // Daily credits exhausted. Common on the free tier and worth naming exactly,
        // because otherwise every submission silently marks itself wrong.
        if (statusCode == 429) {
            LOGGER.error("JDoodle daily credit limit reached");
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("The daily code-execution limit has been reached. "
                            + "Please try again tomorrow or upgrade the JDoodle plan.")
                    .build();
        }

        if ((error != null && !error.isBlank()) || statusCode >= 400) {
            LOGGER.error("JDoodle rejected the request (statusCode={})", statusCode);
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.ERROR)
                    .message("The execution service rejected this request. "
                            + "Check the JDoodle credentials and remaining daily credits.")
                    .build();
        }

        String output = json.path("output").asText("");
        boolean ranAtAll = !json.path("cpuTime").isNull() && !json.path("cpuTime").asText("").isBlank();

        Integer runtimeMs = null;
        String cpuTime = json.path("cpuTime").asText(null);
        if (cpuTime != null && !cpuTime.isBlank()) {
            try {
                runtimeMs = (int) Math.round(Double.parseDouble(cpuTime) * 1000);
            } catch (NumberFormatException ignored) {
                // timing is informational only
            }
        }

        if (output.contains("JDoodle - Timeout") || output.contains("Execution Timed Out")) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.TIME_LIMIT_EXCEEDED)
                    .message("Your program took too long to finish.")
                    .runtimeMs(runtimeMs)
                    .build();
        }

        // Never ran, but produced text: that text is the compiler telling the student why.
        if (!ranAtAll && !output.isBlank()) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.COMPILE_ERROR)
                    .message(output.strip())
                    .build();
        }

        return ExecutionResult.builder()
                .status(ExecutionResult.Status.SUCCESS)
                .stdout(output)
                .runtimeMs(runtimeMs)
                .build();
    }
}
