package com.learntrix.edtech.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorResponse {
    private UUID id;
    private String name;
    private String title;
    private String avatarUrl;
    private String bio;
    private Double rating;
    private Integer students;
}
