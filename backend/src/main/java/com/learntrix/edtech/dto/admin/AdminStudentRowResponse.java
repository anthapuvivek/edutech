package com.learntrix.edtech.dto.admin;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStudentRowResponse {
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
}
