package com.learntrix.edtech.dto.crm;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadCommunicationDto {
    private UUID id;
    private UUID leadId;
    private String channel;
    private String summary;
    private String by;
    private Instant at;
}
