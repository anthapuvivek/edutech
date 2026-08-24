package com.learntrix.edtech.processing;

import com.learntrix.edtech.entity.ClassRecording;
import com.learntrix.edtech.entity.RecordingStatus;
import com.learntrix.edtech.repository.ClassRecordingRepository;
import com.learntrix.edtech.service.VideoProcessingService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AwsMediaConvertVideoProcessingService implements VideoProcessingService {

    private final ClassRecordingRepository recordingRepository;

    public AwsMediaConvertVideoProcessingService(ClassRecordingRepository recordingRepository) {
        this.recordingRepository = recordingRepository;
    }

    @Override
    public void startProcessing(UUID recordingId, String storageKey) {
        ClassRecording recording = recordingRepository.findById(recordingId).orElse(null);
        if (recording == null) return;

        // AWS MediaConvert client integration stub
        // Under production, we would invoke: MediaConvertClient.createJob(...)
        // And use webhooks or polling to update recording status.
        
        recording.setStatus(RecordingStatus.PROCESSING);
        recordingRepository.save(recording);
        
        // Mock success transition for AWS mode when credentials are missing
        recording.setStatus(RecordingStatus.READY);
        recordingRepository.save(recording);
    }
}
