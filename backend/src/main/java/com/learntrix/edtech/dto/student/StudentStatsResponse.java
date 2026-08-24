package com.learntrix.edtech.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatsResponse {
    private Integer totalCourses;
    private Integer activeCourses;
    private Integer completedCourses;
    private Integer learningHours;
    private Integer problemsSolved;
    private Integer quizAverage;
    private Integer points;
    private Integer rank;
}
