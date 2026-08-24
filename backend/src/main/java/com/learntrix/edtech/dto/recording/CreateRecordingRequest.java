package com.learntrix.edtech.dto.recording;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateRecordingRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    @NotNull(message = "Module ID is required")
    private UUID moduleId;

    @NotNull(message = "Lesson ID is required")
    private UUID lessonId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Class date is required")
    private Instant classDate;
}
