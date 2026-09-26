package com.learntrix.edtech.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseAnnouncementRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    /** Null = whole course; set = that cohort only. Verified against the caller. */
    private UUID batchId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @Builder.Default
    private String priority = "NORMAL";
}
