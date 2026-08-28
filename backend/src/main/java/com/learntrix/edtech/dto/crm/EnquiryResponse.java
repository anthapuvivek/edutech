package com.learntrix.edtech.dto.crm;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryResponse {
    private UUID id;
    private String status;
    private Instant createdAt;
}
