package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.util.List;

/** Result of Run Code: sample cases only, never persisted. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RunCodeResponse {
    private String status;
    private String message;
    private Integer passedCount;
    private Integer totalCount;
    private List<TestCaseResultResponse> results;
}
