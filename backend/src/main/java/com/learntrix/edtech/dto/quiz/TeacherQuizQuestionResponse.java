package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.util.List;
import java.util.UUID;

/** Teacher/admin view of a question, with the answer key attached. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherQuizQuestionResponse {
    private UUID id;
    private String questionText;
    private String questionType;
    private Integer points;
    private Integer sequenceNumber;
    private String explanation;
    private List<TeacherQuizOptionResponse> options;
}
