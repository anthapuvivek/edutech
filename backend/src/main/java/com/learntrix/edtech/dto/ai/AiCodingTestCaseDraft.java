package com.learntrix.edtech.dto.ai;

import lombok.*;

/** One generated test case. {@code sample} decides whether a student may ever see it. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiCodingTestCaseDraft {
    private String input;
    private String expectedOutput;
    private boolean sample;
}
