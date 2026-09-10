package com.learntrix.edtech.dto.teacher;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherStudentResponse {
    private UUID id;
    /** Human-readable identifier from StudentProfile, e.g. LTX-2026-0001. */
    private String studentId;
    private String name;
    private String email;
    private String courseTitle;
    /** Batch the enrolment is pinned to; null when the student is enrolled course-wide. */
    private String batchName;
    private int quizzesAttempted;
    private String enrolledAt;
    private int progressPercent;
    private int lessonsCompleted;
    private int quizScore;
    private String assignments;
    private int problemsSolved;
    private int points;
    private int rank;
    private String lastActive;
    private String status;
}
