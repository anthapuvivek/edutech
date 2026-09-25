package com.learntrix.edtech.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

/** Teacher input for one option. `correct` is the answer key and is never echoed to students. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizOptionRequest {

    /** Present when editing an existing option; null when adding a new one. */
    private UUID id;

    @NotBlank(message = "Option text is required")
    private String optionText;

    private boolean correct;

    private Integer sequenceNumber;
}
