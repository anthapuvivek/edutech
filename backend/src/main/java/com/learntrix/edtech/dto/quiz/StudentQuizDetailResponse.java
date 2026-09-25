package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.util.List;

/** A quiz as a student sees it while attempting: metadata plus questions without answers. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentQuizDetailResponse {
    private QuizResponse quiz;
    private List<StudentQuizQuestionResponse> questions;
}
