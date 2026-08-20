package com.learntrix.edtech.common.response;

import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ApiErrorResponse {
    @Builder.Default
    private final boolean success = false;
    
    private final ErrorDetail error;
    
    @Builder.Default
    private final Instant timestamp = Instant.now();
    
    @Builder.Default
    private final String traceId = MDC.get("traceId");

    @Getter
    @Builder
    public static class ErrorDetail {
        private final String code;
        private final String message;
        private final Map<String, String> fieldErrors;
    }

    public static ApiErrorResponse of(String code, String message) {
        return ApiErrorResponse.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }

    public static ApiErrorResponse ofValidation(String code, Map<String, String> fieldErrors) {
        return ApiErrorResponse.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message("Validation failed")
                        .fieldErrors(fieldErrors)
                        .build())
                .build();
    }
}
