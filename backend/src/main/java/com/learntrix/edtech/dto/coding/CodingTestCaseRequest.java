package com.learntrix.edtech.dto.coding;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingTestCaseRequest {
    private String inputData;
    @NotNull(message = "Expected output is required")
    private String expectedOutput;
    /** Sample cases are shown to students and used by Run Code. Hidden ones never are. */
    private boolean sample;
    private Integer sequenceNumber;
    private Integer points;
}
