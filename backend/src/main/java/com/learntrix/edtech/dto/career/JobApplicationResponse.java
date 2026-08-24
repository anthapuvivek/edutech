package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationResponse {
    private UUID id;
    private UUID jobId;
    private String companyName;
    private String role;
    private String appliedAt;
    private String status;
    private String nextAction;
    private String deadline;
    private String interviewAt;
}
