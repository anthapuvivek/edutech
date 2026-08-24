package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerDashboardResponse {
    private CareerReadinessResponse readiness;
    private String placementStatus;
    private List<JobResponse> recommendedJobs;
    private List<JobResponse> recommendedInternships;
    private List<JobApplicationResponse> applications;
    private List<JobApplicationResponse> upcomingInterviews;
    private List<ReferralOpportunityResponse> referralOpportunities;
    private List<JobResponse> savedJobs;
    private List<String> skillsToImprove;
    private List<ChecklistItem> checklist;
    private List<Notification> notifications;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChecklistItem {
        private String label;
        private String status;
        private String detail;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Notification {
        private String id;
        private String kind;
        private String title;
        private String createdAt;
    }
}
