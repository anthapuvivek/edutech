package com.learntrix.edtech.dto.recording;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class UpdateRecordingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Instant classDate;
}
