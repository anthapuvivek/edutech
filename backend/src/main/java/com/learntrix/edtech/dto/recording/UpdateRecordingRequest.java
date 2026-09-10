package com.learntrix.edtech.dto.recording;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UpdateRecordingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Instant classDate;

    /**
     * Curriculum mapping, applied only when {@link #remapCurriculum} is true. This lets a
     * recording that was uploaded before the curriculum existed be filed against a module
     * and lesson afterwards, instead of being stuck unmapped forever.
     */
    private UUID moduleId;

    private UUID lessonId;

    /**
     * Opt-in flag for the mapping fields. Without it a plain title/description edit that
     * omits moduleId and lessonId would silently clear an existing mapping.
     */
    private boolean remapCurriculum;
}
