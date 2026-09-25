package com.learntrix.edtech.dto.ai;

import lombok.*;
import java.util.List;

/** One generated coding problem, before any teacher has approved it. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiCodingProblemDraft {
    private String title;
    private String description;
    private String language;
    private String difficulty;
    private List<String> constraints;
    private String sampleInput;
    private String sampleOutput;
    private List<AiCodingTestCaseDraft> testCases;
}
