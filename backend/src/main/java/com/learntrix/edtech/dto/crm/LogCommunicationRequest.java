package com.learntrix.edtech.dto.crm;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogCommunicationRequest {

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "Summary is required")
    private String summary;
}
