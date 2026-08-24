package com.learntrix.edtech.dto.mentor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorStatsResponse {
    private int totalStudents;
    private int activeStudents;
    private int upcomingSessions;
    private int pendingTasks;
    private int studentsAtRisk;
    private double averageCareerReadiness;
    private double averageProgress;
}
