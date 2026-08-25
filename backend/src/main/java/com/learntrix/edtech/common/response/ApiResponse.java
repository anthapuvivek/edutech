package com.learntrix.edtech.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;

    // Always emitted, even when null. The client unwraps this envelope by looking for
    // the "data" key; letting NON_NULL drop it would hand the caller the whole envelope
    // instead of the null payload for every void endpoint.
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final T data;

    private final String message;
    
    @Builder.Default
    private final Instant timestamp = Instant.now();
    
    @Builder.Default
    private final String traceId = MDC.get("traceId");

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
