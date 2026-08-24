package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralOpportunityResponse {
    private UUID id;
    private String companyName;
    private String role;
    private String location;
    private String referralType;
    private String eligibility;
    private String deadline;
    private int seatsAvailable;
    private boolean verified;
}
