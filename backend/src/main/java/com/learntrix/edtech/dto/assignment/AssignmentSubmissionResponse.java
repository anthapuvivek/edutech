package com.learntrix.edtech.dto.assignment;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionResponse {
    private UUID id;
    private UUID assignmentId;
    private String assignmentTitle;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String submissionUrl;
    private Instant submittedAt;
    private Integer grade;
    private String feedback;
    private String status;
}
