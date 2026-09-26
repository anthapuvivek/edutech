package com.learntrix.edtech.dto.ai;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * Generated drafts returned for review.
 *
 * <p>Nothing in here has been written to the database. The teacher edits, deletes or
 * regenerates, then explicitly adds what they want through the existing quiz and coding
 * APIs - which is also where publication validation happens.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiGenerationResponse {
    /** QUIZ or CODING. */
    private String type;

    /** Echoed back so the UI can confirm which cohort the drafts were generated for. */
    private UUID courseId;
    private String courseTitle;
    private UUID batchId;
    private String batchName;

    private List<AiQuizQuestionDraft> questions;
    private List<AiCodingProblemDraft> problems;
    private List<AiAssignmentDraft> assignments;

    /** Non-fatal notes, e.g. that fewer items came back than were asked for. */
    private List<String> warnings;
}
