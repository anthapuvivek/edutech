package com.learntrix.edtech.dto.ai;

import lombok.*;
import java.util.List;

/**
 * One generated MCQ, before any teacher has approved it.
 *
 * <p>A draft only. Nothing here is persisted until the teacher explicitly adds it through
 * the existing quiz API, which re-validates everything independently.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiQuizQuestionDraft {
    private String question;
    private List<String> options;
    /** Zero-based index into {@code options}. */
    private Integer correctOption;
    private Integer marks;
    private String difficulty;
    private String explanation;
}
