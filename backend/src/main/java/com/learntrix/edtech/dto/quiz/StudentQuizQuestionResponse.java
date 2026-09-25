package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.util.List;
import java.util.UUID;

/** Student view of a question. Carries no answer key and no explanation until submission. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentQuizQuestionResponse {
    private UUID id;
    private String questionText;
    private String questionType;
    private Integer points;
    private Integer sequenceNumber;
    private List<StudentQuizOptionResponse> options;
}
