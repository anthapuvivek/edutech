package com.learntrix.edtech.dto.crm;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddNoteRequest {
    @NotBlank(message = "Note body is required")
    private String body;
}
