package com.learntrix.edtech.dto.mentor;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorSessionResponse {
    private UUID id;
    private String title;
    private String kind;
    private UUID mentorId;
    private UUID studentId;
    private String studentName;
    private String date;
    private String time;
    private int durationMinutes;
    private String platform;
    private String meetingUrl;
    private String agenda;
    private String notes;
    private String status;
}
