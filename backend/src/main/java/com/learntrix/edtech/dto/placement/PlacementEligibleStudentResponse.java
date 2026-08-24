package com.learntrix.edtech.dto.placement;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementEligibleStudentResponse {
    private UUID id;
    private String name;
    private String courseTitle;
    private String batchName;
    private List<String> skills;
    private double attendancePercent;
    private double progressPercent;
    private double codingScore;
    private String resumeStatus;
    private int careerReadiness;
    private String eligibility;
    private String placementStatus;
}
