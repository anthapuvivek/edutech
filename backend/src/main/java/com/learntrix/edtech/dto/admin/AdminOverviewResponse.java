package com.learntrix.edtech.dto.admin;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOverviewResponse {
    private Metrics metrics;
    private List<StudentGrowth> studentGrowth;
    private List<AttendanceTrend> attendanceTrend;
    private List<Performance> performance;
    private List<PlacementFunnelStage> placementFunnel;
    private List<Operation> todaysOperations;
    private List<Alert> alerts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metrics {
        private int students;
        private int activeStudents;
        private int inactiveStudents;
        private int teachers;
        private int activeCourses;
        private int activeBatches;
        private int enrollments;
        private int todaysClasses;
        private int todaysAttendance;
        private int careerEligible;
        private int careerIneligible;
        private int activeJobs;
        private int applications;
        private int interviews;
        private int offers;
        private int placements;
        private double revenueThisMonth;
        private int newEnquiries;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentGrowth {
        private String month;
        private int students;
        private int enrollments;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceTrend {
        private String week;
        private double attendance;
        private double completion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Performance {
        private String label;
        private double quiz;
        private double coding;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlacementFunnelStage {
        private String stage;
        private int value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Operation {
        private String id;
        private String label;
        private String detail;
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Alert {
        private String id;
        private String kind;
        private String title;
        private String message;
        private String age;
    }
}
