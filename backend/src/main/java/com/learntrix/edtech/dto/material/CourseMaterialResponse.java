package com.learntrix.edtech.dto.material;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID teacherId;
    private String teacherName;
    private String title;
    private String description;
    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;
    private Instant createdAt;
}
