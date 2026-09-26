package com.learntrix.edtech.dto.ai;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * What a teacher asks the assistant to generate.
 *
 * <p>Carries no teacher id by design. Identity comes from the JWT; a client-supplied
 * teacherId would be an impersonation vector, so there is nowhere to put one.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiGenerationRequest {

    @NotNull(message = "Course is required")
    private UUID courseId;

    /** Null means course-wide, matching how quizzes and coding problems are scoped. */
    private UUID batchId;

    /** QUIZ or CODING. */
    @NotBlank(message = "Content type is required")
    private String type;

    /** EASY, MEDIUM or HARD. */
    private String difficulty;

    @NotBlank(message = "Topic is required")
    @Size(max = 200, message = "Topic must be 200 characters or fewer")
    private String topic;

    /** Capped so one request cannot tie up the provider - and the teacher - indefinitely. */
    @Min(value = 1, message = "Generate at least one item")
    @Max(value = 10, message = "Generate at most ten items at a time")
    private Integer count;

    @Size(max = 1000, message = "Instructions must be 1000 characters or fewer")
    private String instructions;

    /** Programming language for CODING requests; ignored for QUIZ. */
    private String language;

    /**
     * Regeneration context: what the teacher is currently looking at, plus what to change.
     * Sent by the client rather than stored, so there is no AI conversation table.
     */
    private List<AiQuizQuestionDraft> existingQuestions;
    private List<AiCodingProblemDraft> existingProblems;
    private List<AiAssignmentDraft> existingAssignments;
    private String regenerateInstruction;
}
