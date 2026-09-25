package com.learntrix.edtech.dto.coding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCodingProblemRequest {
    @NotNull(message = "Course ID is required")
    private UUID courseId;
    /** Null = course-wide. When set, the server verifies it belongs to the course and to you. */
    private UUID batchId;
    private UUID moduleId;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;
    private String difficulty;
    private String constraintsText;
    private String starterCode;
    private String language;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    /** DRAFT or PUBLISHED; defaults to DRAFT. */
    private String status;
}
