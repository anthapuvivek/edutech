package com.learntrix.edtech.dto.mentor;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorResponse {
    private UUID id;
    private String name;
    private String email;
    private String avatarUrl;
    private String phone;
}
