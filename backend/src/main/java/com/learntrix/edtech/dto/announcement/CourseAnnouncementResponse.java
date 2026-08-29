package com.learntrix.edtech.dto.announcement;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnnouncementResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID teacherId;
    private String teacherName;
    private String title;
    private String content;
    private String priority;
    private Instant createdAt;
}
