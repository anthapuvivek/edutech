package com.learntrix.edtech.execution;

/**
 * Runs untrusted student code somewhere that is not this JVM.
 *
 * <p>The whole point of this interface is that no implementation may use
 * {@code Runtime.exec}, {@code ProcessBuilder}, a scripting engine, or any other in-process
 * evaluation. Student code executes in an isolated sandbox owned by an external service.</p>
 *
 * <p>Judge0 is the first implementation; the interface exists so it can be replaced (a
 * self-hosted Judge0, a container judge) without touching controllers, services or UI.</p>
 */
public interface CodeExecutionClient {

    /**
     * @param language     language key, e.g. "java", "python", "cpp"
     * @param sourceCode   the student's program
     * @param stdin        input fed to the program, may be null
     * @param timeLimitMs  wall-clock ceiling
     * @param memoryLimitMb memory ceiling
     */
    ExecutionResult execute(String language, String sourceCode, String stdin,
                            int timeLimitMs, int memoryLimitMb);

    /** False when no execution service is configured, so callers can fail gracefully. */
    boolean isConfigured();
}
