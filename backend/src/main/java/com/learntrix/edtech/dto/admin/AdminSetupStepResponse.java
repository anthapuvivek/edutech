package com.learntrix.edtech.dto.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSetupStepResponse {
    private String id;
    private String label;
    private String description;
    private boolean complete;
}
