package com.learntrix.edtech.dto.coding;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RunCodeRequest {
    @NotBlank(message = "Source code is required")
    private String sourceCode;
    private String language;
}
