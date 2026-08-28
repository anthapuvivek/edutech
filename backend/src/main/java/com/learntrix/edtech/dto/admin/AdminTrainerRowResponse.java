package com.learntrix.edtech.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTrainerRowResponse {
    private UUID id;
    private String employeeId;
    private String name;
    private String email;
    private String phone;
    private String headline;
    private String department;
    private String approvalStatus; // "pending" | "approved" | "rejected"
    private List<String> skills;
    private List<String> courses;
    private int batches;
    private int students;
    private int classesThisMonth;
    private double rating;
    private int experienceYears;
    private String status; // "active" | "inactive" | "pending"
    private String onboardingStatus; // "INVITED" | "ACTIVE"
    private String joinedAt;
}
