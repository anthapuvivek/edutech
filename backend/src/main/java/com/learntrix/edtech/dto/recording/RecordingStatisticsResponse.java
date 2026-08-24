package com.learntrix.edtech.dto.recording;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingStatisticsResponse {
    private UUID recordingId;
    private int totalStudents;
    private int studentsStarted;
    private int studentsCompleted;
    private double averageWatchPercentage;
    private double averageWatchDuration;
    private double completionRate;
}
