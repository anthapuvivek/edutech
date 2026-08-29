package com.learntrix.edtech.dto.admin;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAllocationResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private UUID courseId;
    private String courseTitle;
    private String courseSlug;
    private UUID teacherId;
    private String teacherName;
    private UUID batchId;
    private String batchName;
    private String status;
    private Instant createdAt;
}
