package com.learntrix.edtech.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.ai.AiGenerationRequest;
import com.learntrix.edtech.dto.ai.AiGenerationResponse;
import com.learntrix.edtech.service.AiQuestionGenerationService;

import jakarta.validation.Valid;

/**
 * AI Question Assistant - teachers only.
 *
 * <p>Follows the same split as every other controller here: {@code @PreAuthorize} gates the
 * role, and course plus batch ownership is settled inside the service. A student reaching
 * this endpoint is rejected by the annotation before any prompt is built, so an unauthorized
 * call costs nothing and reveals nothing.</p>
 *
 * <p>Generation returns drafts only. Saving happens afterwards through the existing quiz and
 * coding endpoints, so the assistant can never publish anything by itself.</p>
 */
@RestController
@RequestMapping("/api")
public class AiAssistantController {

    private final AiQuestionGenerationService aiService;

    public AiAssistantController(AiQuestionGenerationService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/teacher/ai/question-assistant")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AiGenerationResponse> generate(
            @Valid @RequestBody AiGenerationRequest request) {
        // The teacher id comes from the token. The request body has no field for one.
        UUID teacherId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(aiService.generate(request, teacherId));
    }
}
