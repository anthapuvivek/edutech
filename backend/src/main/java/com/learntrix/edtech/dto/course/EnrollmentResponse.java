package com.learntrix.edtech.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private UUID id;
    private UUID courseId;
    private String courseSlug;
    private String courseTitle;
    private String instructorName;
    private String thumbnailUrl;
    private String status;
    private Integer progressPercent;
    private Integer lessonsCompleted;
    private Integer lessonsTotal;
    private String lastLessonTitle;
    private String nextLessonTitle;
    private Instant enrolledAt;
    private Instant expiresAt;
}
