package com.learntrix.edtech.dto.placement;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementAnalyticsResponse {
    private int totalEligible;
    private int totalApplied;
    private int shortlisted;
    private int interviewed;
    private int offers;
    private int placed;
    private List<FunnelPoint> funnel;
    private List<DriveProgress> drivesProgress;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FunnelPoint {
        private String stage;
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriveProgress {
        private String company;
        private String role;
        private int registered;
        private int shortlisted;
        private int offers;
        private String stage;
    }
}
