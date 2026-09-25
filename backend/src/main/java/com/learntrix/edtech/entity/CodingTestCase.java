package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * One test case for a {@link CodingProblem}.
 *
 * <p>{@code sample} is the disclosure boundary: sample cases are shown to the student and
 * drive Run Code; hidden cases drive Submit only and must never be serialised into a
 * student-facing response - neither their input nor their expected output.</p>
 */
@Entity
@Table(name = "coding_test_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private CodingProblem problem;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "is_sample", nullable = false)
    @Builder.Default
    private boolean sample = false;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Integer sequenceNumber = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 1;
}
