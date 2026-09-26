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
    private UUID batchId;
    private String batchName;
    private String title;
    private String content;
    private String priority;
    private Instant createdAt;

    /** Student view: whether this student has opened it. Null in teacher listings. */
    private Boolean read;
    /** Teacher view: how many students have opened it. */
    private Integer readCount;
}
