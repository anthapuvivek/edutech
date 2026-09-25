package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.util.UUID;

/** Teacher view: includes hidden cases and their expected output. Never sent to students. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherCodingTestCaseResponse {
    private UUID id;
    private String inputData;
    private String expectedOutput;
    private boolean sample;
    private Integer sequenceNumber;
    private Integer points;
}
