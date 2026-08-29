package com.learntrix.edtech.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAssignmentRequest {

    @NotBlank(message = "Submission URL or repository link is required")
    private String submissionUrl;

    private String notes;
}
