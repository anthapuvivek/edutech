package com.learntrix.edtech.dto.coding;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDate;

/**
 * Teacher input for assigning an externally hosted problem.
 *
 * <p>No teacherId field: identity comes from the JWT. Course and batch are checked against
 * what the caller may actually manage before anything is stored.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalCodingProblemRequest {

    @NotNull(message = "Course is required")
    private UUID courseId;

    @NotNull(message = "Batch is required")
    private UUID batchId;

    @NotBlank(message = "Platform is required")
    private String platform;

    /** Optional - some platforms do not number their problems. */
    @Size(max = 40, message = "Problem number must be 40 characters or fewer")
    private String problemNumber;

    /**
     * Required, and validated server-side as http/https.
     *
     * <p>Never derived from the problem number - LeetCode #1 is /problems/two-sum/.</p>
     */
    @NotBlank(message = "Problem URL is required")
    private String problemUrl;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or fewer")
    private String title;

    /** EASY, MEDIUM, HARD or UNKNOWN. */
    private String difficulty;

    /** Comma-separated, e.g. "Arrays, Hash Table". */
    private String topics;

    /** Teacher's own notes. Must not be copied from the external platform. */
    private String description;

    private LocalDate deadline;

    /** true publishes it to students, false keeps it as a draft. */
    private Boolean active;
}
