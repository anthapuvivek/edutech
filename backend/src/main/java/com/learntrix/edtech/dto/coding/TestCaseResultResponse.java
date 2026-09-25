package com.learntrix.edtech.dto.coding;

import lombok.*;

/**
 * One test case outcome.
 *
 * <p>For Run Code (sample cases) input and expected output are included, because the student
 * can already see them. For Submit they are left null so hidden cases stay hidden.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestCaseResultResponse {
    private Integer sequenceNumber;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private boolean passed;
    private String status;
    private String message;
    private Integer runtimeMs;
}
