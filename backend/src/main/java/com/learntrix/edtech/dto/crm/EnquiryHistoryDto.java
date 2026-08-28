package com.learntrix.edtech.dto.crm;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryHistoryDto {
    private UUID id;
    private String subject;
    private Instant at;
}
