package com.learntrix.edtech.ai;

import java.util.Map;

/**
 * Generates structured JSON from a language model.
 *
 * <p>Deliberately narrow and provider-agnostic, mirroring {@code CodeExecutionClient}: the
 * caller supplies a schema and gets JSON text back, and nothing about Gemini leaks into the
 * services above. Swapping providers should not touch a controller, service or DTO.</p>
 *
 * <p>The implementation holds the credential. It is never passed in, never returned, and
 * never appears in a response, a log line or an exception message.</p>
 */
public interface AiProvider {

    /**
     * @param systemInstruction how the model should behave
     * @param userPrompt        what to generate
     * @param responseSchema    JSON schema the reply must conform to
     * @return the model's reply as JSON text - still untrusted, still to be validated
     */
    String generateJson(String systemInstruction, String userPrompt, Map<String, Object> responseSchema);

    /** False when no API key is configured, so callers can fail with a clear message. */
    boolean isConfigured();
}
