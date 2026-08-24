package com.learntrix.edtech.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityItemResponse {
    private UUID id;
    private String kind;
    private String title;
    private Integer points;
    private Instant occurredAt;
}
