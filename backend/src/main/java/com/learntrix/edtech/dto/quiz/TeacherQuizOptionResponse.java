package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.util.UUID;

/** Teacher/admin view of an option. Includes `correct`; never returned to a student. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherQuizOptionResponse {
    private UUID id;
    private String optionText;
    private boolean correct;
    private Integer sequenceNumber;
}
