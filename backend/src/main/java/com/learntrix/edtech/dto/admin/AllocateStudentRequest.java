package com.learntrix.edtech.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocateStudentRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    private UUID teacherId;

    private UUID batchId;

    private String status; // ACTIVE, ENROLLED, COMPLETED, INACTIVE
}
