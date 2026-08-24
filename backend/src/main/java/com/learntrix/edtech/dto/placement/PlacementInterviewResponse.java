package com.learntrix.edtech.dto.placement;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementInterviewResponse {
    private UUID id;
    private String studentName;
    private UUID studentId;
    private String companyName;
    private String role;
    private String round;
    private String date;
    private String time;
    private String interviewer;
    private String platform;
    private String meetingUrl;
    private String status;
}
