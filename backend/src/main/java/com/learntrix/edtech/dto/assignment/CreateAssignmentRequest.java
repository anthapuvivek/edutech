package com.learntrix.edtech.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    /** Null = course-wide; set = that cohort only. Verified against the caller. */
    private UUID batchId;

    /** DRAFT or PUBLISHED. Defaults to DRAFT so nothing goes live by accident. */
    private String status;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Instant dueDate;

    @Builder.Default
    private Integer points = 100;
}
