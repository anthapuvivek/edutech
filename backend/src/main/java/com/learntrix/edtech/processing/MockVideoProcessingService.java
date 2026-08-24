package com.learntrix.edtech.processing;

import com.learntrix.edtech.entity.ClassRecording;
import com.learntrix.edtech.entity.RecordingStatus;
import com.learntrix.edtech.repository.ClassRecordingRepository;
import com.learntrix.edtech.service.VideoProcessingService;
import com.learntrix.edtech.service.VideoStorageService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class MockVideoProcessingService implements VideoProcessingService {

    private final ClassRecordingRepository recordingRepository;
    private final VideoStorageService videoStorageService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public MockVideoProcessingService(
            ClassRecordingRepository recordingRepository,
            @Lazy VideoStorageService videoStorageService) {
        this.recordingRepository = recordingRepository;
        this.videoStorageService = videoStorageService;
    }

    @Override
    public void startProcessing(UUID recordingId, String storageKey) {
        ClassRecording recording = recordingRepository.findById(recordingId).orElse(null);
        if (recording == null) return;

        recording.setStatus(RecordingStatus.PROCESSING);
        recordingRepository.save(recording);

        // Schedule async transition to READY after 8 seconds
        scheduler.schedule(() -> {
            try {
                ClassRecording rec = recordingRepository.findById(recordingId).orElse(null);
                if (rec == null) return;

                rec.setStatus(RecordingStatus.READY);
                rec.setVideoUrl(videoStorageService.getVideoUrl(storageKey));
                rec.setHlsManifestUrl(videoStorageService.getHlsManifestUrl(storageKey));
                rec.setDurationSeconds(1800 + (int)(Math.random() * 3600)); // Random mock duration between 30m and 1.5h
                rec.setFileSizeBytes(104857600L + (long)(Math.random() * 524288000L)); // Random mock file size between 100MB and 600MB
                rec.setVideoFormat("mp4");
                rec.setThumbnailUrl("https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70");
                rec.setUpdatedAt(Instant.now());
                recordingRepository.save(rec);
            } catch (Exception e) {
                // Set FAILED if something goes wrong
                ClassRecording rec = recordingRepository.findById(recordingId).orElse(null);
                if (rec != null) {
                    rec.setStatus(RecordingStatus.FAILED);
                    recordingRepository.save(rec);
                }
            }
        }, 8, TimeUnit.SECONDS);
    }
}
