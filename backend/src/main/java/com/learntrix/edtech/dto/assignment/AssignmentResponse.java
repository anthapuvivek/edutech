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
    private UUID batchId;
    private String batchName;
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

    /** Teacher listings only - counts of DISTINCT students, computed server-side. */
    private Integer totalStudents;
    private Integer submittedCount;
    private Integer pendingCount;
    private Integer lateCount;
    private Integer gradedCount;
}
