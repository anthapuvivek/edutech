package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerProfileResponse {
    private String studentId;
    private String name;
    private String headline;
    private String education;
    private String courseTitle;
    private List<String> skills;
    private List<String> preferredRoles;
    private List<String> preferredLocations;
    private String workMode;
    private String experienceLevel;
    private String expectedSalary;
    private String availability;
    private Links links;
    private int completionPercent;
    private List<String> missingItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Links {
        private String linkedin;
        private String github;
        private String portfolio;
        private String leetcode;
        private String hackerrank;
    }
}
