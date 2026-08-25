package com.learntrix.edtech.dto.live;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassResponse {
    private UUID id;
    private String title;
    private String courseTitle;
    private UUID batchId;
    private String batchName;
    private String trainerName;
    private String description;
    private String date;        // YYYY-MM-DD
    private String startTime;   // HH:MM
    private String endTime;     // HH:MM
    private String platform;
    private String meetingUrl;
    private String type;
    private String visibility;
    private Boolean published;
    private String status;
    private Integer registrations;
}
