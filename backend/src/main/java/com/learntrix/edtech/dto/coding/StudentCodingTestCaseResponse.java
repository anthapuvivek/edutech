package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.util.UUID;

/**
 * Student view of a SAMPLE test case only.
 *
 * <p>Hidden cases are never mapped into this type. The service filters to samples before
 * mapping, so a hidden input or expected output cannot reach a student response.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentCodingTestCaseResponse {
    private UUID id;
    private String inputData;
    private String expectedOutput;
    private Integer sequenceNumber;
}
