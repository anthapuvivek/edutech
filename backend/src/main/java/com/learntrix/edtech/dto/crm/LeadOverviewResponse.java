package com.learntrix.edtech.dto.crm;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadOverviewResponse {
    private long totalLeads;
    private long newLeads;
    private long followUpsToday;
    private long demoRegistrations;
    private long demoAttendees;
    private long paymentPending;
    private long convertedLeads;
    private double conversionRate;
}
