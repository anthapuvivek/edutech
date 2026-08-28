package com.learntrix.edtech.dto.crm;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadFollowUpDto {
    private UUID id;
    private UUID leadId;
    private String date;
    private String time;
    private String notes;
    private String nextAction;
    private String assignedTo;
    private String status;
}
