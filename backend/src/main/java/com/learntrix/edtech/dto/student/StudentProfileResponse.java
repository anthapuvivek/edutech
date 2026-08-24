package com.learntrix.edtech.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {
    private UUID id;
    private String studentId;
    private String name;
    private String avatarUrl;
    private String level;
    private String nextLevel;
    private Integer points;
    private Integer pointsToNextLevel;
    private Integer nextLevelThreshold;
    private Integer rank;
    private Integer streakDays;
}
