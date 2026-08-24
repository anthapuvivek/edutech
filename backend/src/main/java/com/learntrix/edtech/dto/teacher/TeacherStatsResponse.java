package com.learntrix.edtech.dto.teacher;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherStatsResponse {
    private int totalStudents;
    private int activeStudents;
    private int courses;
    private double averageCompletion;
    private double averageQuizScore;
    private int problemsSolved;
    private double engagement;
    private int upcomingClasses;
}
