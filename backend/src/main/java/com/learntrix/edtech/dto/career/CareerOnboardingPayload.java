package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerOnboardingPayload {
    private String targetRole;
    private String preferredLocation;
    private String experienceLevel;
    private String workMode;
    private List<String> targetCompanies;
    private String careerGoal;
}
