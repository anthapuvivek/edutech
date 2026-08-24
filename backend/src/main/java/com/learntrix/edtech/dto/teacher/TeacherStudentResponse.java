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
    private String name;
    private String email;
    private String courseTitle;
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
