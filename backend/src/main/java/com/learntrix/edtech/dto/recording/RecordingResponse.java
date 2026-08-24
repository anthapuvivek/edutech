package com.learntrix.edtech.dto.recording;

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
public class RecordingResponse {
    private UUID id;
    
    private UUID courseId;
    private String courseTitle;
    
    private UUID moduleId;
    private String moduleTitle;
    
    private UUID lessonId;
    private String lessonTitle;
    
    private UUID teacherId;
    private String teacherName;
    
    private String title;
    private String description;
    
    private Instant classDate;
    
    private String videoStorageKey;
    private String videoUrl;
    private String hlsManifestUrl;
    private String thumbnailUrl;
    
    private Integer durationSeconds;
    private Long fileSizeBytes;
    private String videoFormat;
    
    private String status;
    private boolean published;
    
    private Instant createdAt;
    private Instant publishedAt;
}
