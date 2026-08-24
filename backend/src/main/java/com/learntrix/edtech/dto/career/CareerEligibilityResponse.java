package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerEligibilityResponse {
    private String state;
    private String reason;
    private List<Program> eligiblePrograms;
    private boolean onboardingCompleted;
    private String checkedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Program {
        private String courseSlug;
        private String courseTitle;
    }
}
