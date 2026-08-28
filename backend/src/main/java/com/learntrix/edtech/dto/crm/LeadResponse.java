package com.learntrix.edtech.dto.crm;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadResponse {
    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String courseInterest;
    private String source;
    private String assignedTo;
    private String assignedToId;
    private String stage;
    private Instant lastContactAt;
    private Instant nextFollowUpAt;
    private Instant createdAt;
    private String city;
    private Integer budget;
    private Integer score;
}
