package com.learntrix.edtech.dto.recording;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingProgressResponse {
    private Integer watchedSeconds;
    private Integer durationSeconds;
    private boolean completed;
    private Instant lastWatchedAt;
}
