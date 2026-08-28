package com.learntrix.edtech.dto.crm;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignLeadRequest {
    @NotNull(message = "assignedToId is required")
    private UUID assignedToId;
}
