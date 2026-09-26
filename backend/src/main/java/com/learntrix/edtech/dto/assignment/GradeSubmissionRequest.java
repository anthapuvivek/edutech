package com.learntrix.edtech.dto.assignment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSubmissionRequest {

    @NotNull(message = "Grade is required")
    // Upper bound is the assignment's own points value, checked in the service - a
    // hardcoded 100 here silently accepted 100 on an assignment worth 50.
    @Min(0)
    private Integer grade;

    private String feedback;
}
