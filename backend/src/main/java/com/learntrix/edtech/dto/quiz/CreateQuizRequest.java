package com.learntrix.edtech.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    /**
     * Optional cohort scope. Null = course-wide. When set, the server verifies the batch
     * belongs to this course AND that the caller owns it before accepting.
     */
    private UUID batchId;

    /** DRAFT or PUBLISHED. Defaults to DRAFT so a half-built quiz is never student-visible. */
    private String status;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Builder.Default
    private Integer timeLimitMinutes = 30;

    @Builder.Default
    private Integer passingScore = 60;
}
