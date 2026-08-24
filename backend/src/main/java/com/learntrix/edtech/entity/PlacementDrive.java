package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "placement_drives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "eligibility_summary")
    private String eligibilitySummary;

    @Column(name = "application_deadline")
    private Instant applicationDeadline;

    @Column(name = "assessment_date")
    private Instant assessmentDate;

    @Column(name = "interview_date")
    private Instant interviewDate;

    private Integer openings = 1;

    private String stage = "Draft";

    private String location;

    @Column(name = "ctc_range")
    private String ctcRange;
}
