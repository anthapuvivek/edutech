package com.learntrix.edtech.dto.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptRequest {

    @Min(0)
    @Max(100)
    private Integer score;

    private Map<String, Object> answers;
}
