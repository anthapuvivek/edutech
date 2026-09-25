package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** An assigned external problem, as both portals see it. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalCodingProblemResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID batchId;
    private String batchName;
    private UUID teacherId;
    private String teacherName;

    private String platform;
    private String problemNumber;
    /** The link the student opens in a new tab. */
    private String problemUrl;
    private String title;
    private String difficulty;
    private String topics;
    /** Teacher-authored notes only. */
    private String description;

    /** PUBLISHED = visible to students, DRAFT = hidden. */
    private String status;
    private boolean active;
    private Instant createdAt;
    private LocalDate deadline;

    /** The requesting student's own self-reported state. Null in teacher listings. */
    private Boolean completed;
    private Instant completedAt;
    private Instant openedAt;
    private String progressStatus;

    /** How many students have marked it done. Populated for teachers only. */
    private Integer completedCount;
}
