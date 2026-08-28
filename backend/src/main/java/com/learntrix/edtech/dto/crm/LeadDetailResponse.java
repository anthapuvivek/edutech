package com.learntrix.edtech.dto.crm;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadDetailResponse {
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

    @Builder.Default
    private List<LeadCommunicationDto> communications = new ArrayList<>();

    @Builder.Default
    private List<LeadNoteDto> notes = new ArrayList<>();

    @Builder.Default
    private List<LeadFollowUpDto> followUps = new ArrayList<>();

    private Boolean demoAttended;

    @Builder.Default
    private List<EnquiryHistoryDto> enquiryHistory = new ArrayList<>();

    @Builder.Default
    private List<PaymentHistoryDto> paymentHistory = new ArrayList<>();

    private String enrollmentStatus;
}
