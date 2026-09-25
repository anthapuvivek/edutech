package com.learntrix.edtech.execution;

import lombok.*;

/**
 * Outcome of running one program against one input.
 *
 * <p>Deliberately transport-agnostic: nothing here is Judge0-specific, so swapping the
 * execution backend later changes no caller.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExecutionResult {

    public enum Status {
        SUCCESS,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        /** The execution service itself failed or is not configured. */
        ERROR
    }

    private Status status;
    private String stdout;
    /** Compiler or runtime message. Safe to show a student; carries no test data. */
    private String message;
    private Integer runtimeMs;

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
