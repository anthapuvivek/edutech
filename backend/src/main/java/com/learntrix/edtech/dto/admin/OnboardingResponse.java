package com.learntrix.edtech.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponse {
    private UUID id;
    private UUID userId;
    private String email;
    private String name;
    private String role;
    private String identifier; // studentId or employeeId
    private String onboardingStatus; // INVITED, ACTIVE, PENDING
    private String emailStatus; // SENT, FAILED, QUEUED
    private String message;
    private boolean ok;
}
