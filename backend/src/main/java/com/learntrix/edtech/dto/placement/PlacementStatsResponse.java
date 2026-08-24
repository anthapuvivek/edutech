package com.learntrix.edtech.dto.placement;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementStatsResponse {
    private int eligibleStudents;
    private int activeJobs;
    private int placementDrives;
    private int applications;
    private int shortlisted;
    private int interviews;
    private int offers;
    private int placements;
}
