package com.learntrix.edtech.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseRequest {

    private String title;
    private String slug;
    private String subtitle;
    private String category;
    private String thumbnailUrl;
    private String level;
    private String language;
    private Integer durationHours;
    private Integer lessonCount;
    private Integer price;
    private Integer originalPrice;
    private String currency;
    private String status; // DRAFT, PUBLISHED, ARCHIVED
    private UUID instructorId;
    private List<String> skills;
}
