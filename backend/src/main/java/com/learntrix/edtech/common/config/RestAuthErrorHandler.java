package com.learntrix.edtech.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.common.exception.ErrorCode;
import com.learntrix.edtech.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security-filter-chain error responses.
 *
 * Without this, Spring Security falls back to Http403ForbiddenEntryPoint (because both
 * formLogin and httpBasic are disabled) and answers unauthenticated requests with a bare
 * 403. The client then cannot tell "your session expired, sign in again" apart from
 * "you are signed in but not allowed here", so it can never clear a dead session.
 *
 * Denials raised inside a controller by @PreAuthorize are handled by GlobalExceptionHandler
 * instead; both paths emit the same ApiErrorResponse shape.
 */
@Component
public class RestAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAuthErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** No credentials, or credentials that did not authenticate. */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_INVALID,
                "Your session has expired. Please sign in again.");
    }

    /** Authenticated, but the authorities do not permit this resource. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You don't have permission to access this resource.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(code, message));
    }
}
