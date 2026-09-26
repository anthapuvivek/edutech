package com.learntrix.edtech.dto.ai;

import lombok.*;

/**
 * One generated assignment brief, before any teacher has approved it.
 *
 * <p>A draft only. Nothing is persisted until the teacher explicitly adds it through the
 * existing assignment API, which validates independently and creates it as DRAFT.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiAssignmentDraft {
    private String title;
    /** Teacher-facing instructions. Written by the model, edited by the teacher. */
    private String description;
    private Integer points;
    private String difficulty;
    /** Suggested days from today; the teacher picks the real date. */
    private Integer suggestedDueInDays;
}
