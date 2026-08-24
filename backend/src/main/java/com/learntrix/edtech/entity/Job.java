package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    private String type;

    @Column(name = "work_mode")
    private String workMode;

    private String location;

    @Column(name = "ctc_range")
    private String ctcRange;

    @Column(name = "eligibility_summary")
    private String eligibilitySummary;

    @Column(name = "application_deadline")
    private Instant applicationDeadline;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt = Instant.now();

    private String status = "Active";

    private Integer openings = 1;
}
