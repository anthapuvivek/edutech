package com.learntrix.edtech.dto.crm;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveStageRequest {
    @NotBlank(message = "Stage is required")
    private String stage;
}
