package com.learntrix.edtech.dto.recording;

import lombok.Data;

@Data
public class RecordingProgressRequest {
    private Integer watchedSeconds;
    private Integer durationSeconds;
}
