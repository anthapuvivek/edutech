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

    /**
     * Optional curriculum mapping. Left null when the course has no modules yet, or when
     * the trainer simply wants the recording filed against the course as a whole. When it
     * IS supplied the service still verifies it belongs to the selected course.
     */
    private UUID moduleId;

    /** Optional curriculum mapping - see {@link #moduleId}. */
    private UUID lessonId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    /** Optional; defaults to the time the recording is created. */
    private Instant classDate;
}
