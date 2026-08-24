package com.learntrix.edtech.dto.mentor;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorActionItemResponse {
    private String id;
    private UUID mentorId;
    private UUID studentId;
    private String studentName;
    private String task;
    private String deadline;
    private String status;
}
