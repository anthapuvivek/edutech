package com.learntrix.edtech.dto.mentor;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorNoteResponse {
    private UUID id;
    private UUID mentorId;
    private UUID studentId;
    private String studentName;
    private String category;
    private String body;
    private String at;
    private boolean isPrivate;
}
