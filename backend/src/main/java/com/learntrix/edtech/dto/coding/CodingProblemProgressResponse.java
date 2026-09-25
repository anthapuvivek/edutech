package com.learntrix.edtech.dto.coding;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingProblemProgressResponse {
    private UUID problemId;
    private String problemTitle;
    private int totalStudents;
    private int completed;
    private int inProgress;
    private int notStarted;
    private List<StudentProgress> students;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StudentProgress {
        private UUID studentId;
        private String studentName;
        private String studentEmail;
        private String status;
        private Instant openedAt;
        private Instant completedAt;
    }
}
