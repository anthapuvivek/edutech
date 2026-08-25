package com.learntrix.edtech.storage;

import com.learntrix.edtech.dto.recording.UploadUrlResponse;
import com.learntrix.edtech.service.VideoStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

@Service
public class LocalVideoStorageService implements VideoStorageService {

    private final String uploadDir;
    private final String serverUrl;

    public LocalVideoStorageService(
            @Value("${learntrix.video.local-dir:uploads}") String uploadDir,
            @Value("${learntrix.video.server-url:http://localhost:8081}") String serverUrl) {
        this.uploadDir = uploadDir;
        this.serverUrl = serverUrl;
        
        // Ensure local uploads directory exists
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local upload directory", e);
        }
    }

    @Override
    public UploadUrlResponse createUploadUrl(UUID courseId, UUID recordingId, String fileName) {
        String storageKey = String.format("courses/%s/recordings/%s/original/%s", courseId, recordingId, fileName);
        String uploadUrl = String.format("%s/api/recordings/upload-local?key=%s", serverUrl, storageKey);

        return UploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .storageKey(storageKey)
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour expiration
                .build();
    }

    @Override
    public void deleteVideo(String storageKey) {
        if (storageKey == null) return;
        Path path = Paths.get(uploadDir, storageKey);
        try {
            Files.deleteIfExists(path);
            
            // Try to delete parent folders if empty
            Path parent = path.getParent();
            while (parent != null && !parent.equals(Paths.get(uploadDir))) {
                if (Files.isDirectory(parent) && isEmptyDirectory(parent)) {
                    Files.delete(parent);
                } else {
                    break;
                }
                parent = parent.getParent();
            }
        } catch (IOException e) {
            // Log warning
        }
    }

    private boolean isEmptyDirectory(Path path) throws IOException {
        try (var entries = Files.list(path)) {
            return !entries.findFirst().isPresent();
        }
    }

    @Override
    public String getVideoUrl(String storageKey) {
        if (storageKey == null) return null;
        return String.format("%s/uploads/%s", serverUrl, storageKey);
    }

    @Override
    public String getHlsManifestUrl(String storageKey) {
        if (storageKey == null) return null;
        // In local mock mode, we can point to the original file or a simulated manifest.
        // Let's just point to the original video so the player can actually play it.
        return getVideoUrl(storageKey);
    }

    public void storeFile(String storageKey, byte[] bytes) throws IOException {
        Path uploadDirAbsolute = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetPath = uploadDirAbsolute.resolve(storageKey).normalize();
        // Guard against path traversal: resolved path must be inside uploadDir
        if (!targetPath.startsWith(uploadDirAbsolute)) {
            throw new IllegalArgumentException("Invalid storage key — path traversal detected: " + storageKey);
        }
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, bytes);
    }
}
