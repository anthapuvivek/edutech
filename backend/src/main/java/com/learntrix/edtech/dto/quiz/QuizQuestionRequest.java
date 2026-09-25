package com.learntrix.edtech.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/** Teacher input for a question and the full set of its options. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String questionType;

    private Integer points;

    private Integer sequenceNumber;

    /** Revealed to the student only after they submit. */
    private String explanation;

    @NotEmpty(message = "A question needs at least two options")
    @Valid
    private List<QuizOptionRequest> options;
}
