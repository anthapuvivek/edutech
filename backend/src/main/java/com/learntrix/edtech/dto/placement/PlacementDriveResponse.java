package com.learntrix.edtech.dto.placement;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementDriveResponse {
    private UUID id;
    private String companyName;
    private String role;
    private String description;
    private String eligibilitySummary;
    private String applicationDeadline;
    private String assessmentDate;
    private String interviewDate;
    private int openings;
    private int eligibleStudents;
    private int registeredStudents;
    private String stage;
    private String location;
    private String ctcRange;
}
