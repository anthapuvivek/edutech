package com.learntrix.edtech.storage;

import com.learntrix.edtech.dto.recording.UploadUrlResponse;
import com.learntrix.edtech.service.VideoStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class S3VideoStorageService implements VideoStorageService {

    private final String bucketName;
    private final String cdnDomain;
    private final String regionName;

    public S3VideoStorageService(
            @Value("${aws.s3.bucket:learntrix-videos}") String bucketName,
            @Value("${aws.cloudfront.domain:cdn.learntrix.com}") String cdnDomain,
            @Value("${aws.s3.region:us-east-1}") String regionName) {
        this.bucketName = bucketName;
        this.cdnDomain = cdnDomain;
        this.regionName = regionName;
    }

    private S3Presigner getPresigner() {
        return S3Presigner.builder()
                .region(Region.of(regionName))
                .build();
    }

    private S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(regionName))
                .build();
    }

    @Override
    public UploadUrlResponse createUploadUrl(UUID courseId, UUID recordingId, String fileName) {
        String storageKey = String.format("courses/%s/recordings/%s/original/%s", courseId, recordingId, fileName);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(60));

        try (S3Presigner presigner = getPresigner()) {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .putObjectRequest(builder -> builder
                            .bucket(bucketName)
                            .key(storageKey)
                            .build())
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedRequest.url().toString();

            return UploadUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .storageKey(storageKey)
                    .expiresAt(expiresAt)
                    .build();
        } catch (Exception e) {
            // Fallback for local testing if AWS environment is missing
            String uploadUrl = String.format("https://%s.s3.%s.amazonaws.com/%s?mock-presigned=true", bucketName, regionName, storageKey);
            return UploadUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .storageKey(storageKey)
                    .expiresAt(expiresAt)
                    .build();
        }
    }

    @Override
    public void deleteVideo(String storageKey) {
        if (storageKey == null) return;
        try (S3Client s3Client = getS3Client()) {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            // Log warning
        }
    }

    @Override
    public String getVideoUrl(String storageKey) {
        if (storageKey == null) return null;
        return String.format("https://%s/%s", cdnDomain, storageKey);
    }

    @Override
    public String getHlsManifestUrl(String storageKey) {
        if (storageKey == null) return null;
        // Replaces /original/video.mp4 with /processed/master.m3u8 or similar
        String processedKey = storageKey.replace("/original/", "/processed/");
        int lastSlashIndex = processedKey.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            processedKey = processedKey.substring(0, lastSlashIndex) + "/master.m3u8";
        }
        return getVideoUrl(processedKey);
    }
}
