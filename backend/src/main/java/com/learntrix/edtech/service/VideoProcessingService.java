package com.learntrix.edtech.service;

import java.util.UUID;

public interface VideoProcessingService {
    void startProcessing(UUID recordingId, String storageKey);
}
