package com.learntrix.edtech.dto.admin;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStudentDetailResponse {
    // Fields from AdminStudentRow
    private UUID id;
    private String studentId;
    private String name;
    private String email;
    private String phone;
    private String courseTitle;
    private String batchName;
    private String trainerName;
    private String status;
    private String statusReason;
    private double attendancePercent;
    private double progressPercent;
    private double quizScore;
    private double codingScore;
    private double assignmentCompletion;
    private double profileCompletion;
    private int points;
    private int rank;
    private String careerStatus;
    private String paymentStatus;
    private String placementStatus;
    private String location;
    private String college;
    private int graduationYear;
    private String enrolledAt;

    // Detailed Profile Fields
    private String dateOfBirth;
    private String gender;
    private String qualification;
    private List<String> skills;
    private Links links;
    private List<Document> documents;
    private List<TimelineEntry> timeline;
    private List<EnrollmentDetail> enrollments;
    private List<AttendanceDetail> attendance;
    private List<AssessmentDetail> assessments;
    private List<ApplicationDetail> applications;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Links {
        private String github;
        private String linkedin;
        private String leetcode;
        private String hackerrank;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Document {
        private String id;
        private String kind;
        private String name;
        private String uploadedAt;
        private String reviewStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelineEntry {
        private String id;
        private String stage;
        private String detail;
        private String occurredAt;
        private String state;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentDetail {
        private String id;
        private String courseTitle;
        private String batchName;
        private String trainerName;
        private String startDate;
        private String endDate;
        private String courseStatus;
        private String paymentStatus;
        private boolean careerEligible;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceDetail {
        private String date;
        private String classTitle;
        private String batchName;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssessmentDetail {
        private String id;
        private String kind;
        private String title;
        private double score;
        private String submittedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicationDetail {
        private String id;
        private String company;
        private String role;
        private String appliedAt;
        private String status;
    }
}
