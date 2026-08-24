package com.learntrix.edtech.dto.placement;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementApplicationResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String companyName;
    private String role;
    private String courseTitle;
    private String eligibility;
    private String appliedAt;
    private String status;
    private String interviewAt;
    private String result;
    private UUID driveId;
}
