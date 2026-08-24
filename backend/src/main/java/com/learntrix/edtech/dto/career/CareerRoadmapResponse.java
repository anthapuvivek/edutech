package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerRoadmapResponse {
    private String goal;
    private List<RoadmapStep> steps;
    private List<String> nextRecommended;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadmapStep {
        private String label;
        private int progressPercent;
    }
}
