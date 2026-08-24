package com.learntrix.edtech.service;

import com.learntrix.edtech.dto.recording.UploadUrlResponse;
import java.util.UUID;

public interface VideoStorageService {
    UploadUrlResponse createUploadUrl(UUID courseId, UUID recordingId, String fileName);
    void deleteVideo(String storageKey);
    String getVideoUrl(String storageKey);
    String getHlsManifestUrl(String storageKey);
}
