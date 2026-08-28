package com.learntrix.edtech.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse {
    private int rank;
    private String studentId;
    private String name;
    private int points;
    private int problemsSolved;
    private int quizScore;
    private int attendance;
    private int streak;
    private String level;
    private Boolean isCurrentUser;
}
