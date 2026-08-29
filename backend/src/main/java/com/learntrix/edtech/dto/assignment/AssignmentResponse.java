package com.learntrix.edtech.dto.assignment;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID teacherId;
    private String teacherName;
    private String title;
    private String description;
    private Instant dueDate;
    private Integer points;
    private String status;
    private Instant createdAt;
    private boolean submitted;
    private String submissionStatus;
    private Integer grade;
    private String feedback;
}
