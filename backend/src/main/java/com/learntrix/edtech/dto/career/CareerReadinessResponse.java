package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerReadinessResponse {
    private int overall;
    private List<ComponentScore> components;
    private String updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComponentScore {
        private String label;
        private int score;
    }
}
