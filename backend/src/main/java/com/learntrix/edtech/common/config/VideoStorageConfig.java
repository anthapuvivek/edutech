package com.learntrix.edtech.common.config;

import com.learntrix.edtech.service.VideoStorageService;
import com.learntrix.edtech.storage.LocalVideoStorageService;
import com.learntrix.edtech.storage.S3VideoStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VideoStorageConfig {

    @Bean
    @Primary
    public VideoStorageService videoStorageService(
            @Value("${learntrix.video.storage:local}") String storageType,
            LocalVideoStorageService localService,
            S3VideoStorageService s3Service) {
        if ("s3".equalsIgnoreCase(storageType)) {
            return s3Service;
        }
        return localService;
    }
}
