package com.learntrix.edtech.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private UUID id;
    private String slug;
    private String title;
    private String subtitle;
    private String category;
    private List<String> skills;
    private String level;
    private String language;
    private Integer durationHours;
    private Integer lessonCount;
    private Double rating;
    private Integer ratingCount;
    private Integer studentCount;
    private Integer price;
    private Integer originalPrice;
    private String currency;
    private String thumbnailUrl;
    private String status;
    private InstructorResponse instructor;
    private List<String> badges;
    private Instant updatedAt;
    private List<ModuleResponse> modules;
}
