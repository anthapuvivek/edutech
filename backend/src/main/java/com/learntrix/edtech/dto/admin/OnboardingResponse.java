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
    private String emailStatus; // SENT, FAILED, NOT_CONFIGURED
    // Why the mail did not go out, when emailStatus is not SENT. Lets the admin UI show a
    // real reason instead of a green toast for a message nobody received.
    private String emailError;
    // The activation link that was mailed. Always returned so an admin can hand it over
    // manually while SMTP is being fixed.
    private String activationUrl;
    private String message;
    private boolean ok;
}
