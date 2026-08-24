package com.learntrix.edtech.dto.mentor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorAlertResponse {
    private String id;
    private String kind;
    private String studentName;
    private String detail;
    private String severity;
    private String at;
}
