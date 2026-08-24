package com.learntrix.edtech.common.config;

import com.learntrix.edtech.processing.AwsMediaConvertVideoProcessingService;
import com.learntrix.edtech.processing.MockVideoProcessingService;
import com.learntrix.edtech.service.VideoProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VideoProcessingConfig {

    @Bean
    @Primary
    public VideoProcessingService videoProcessingService(
            @Value("${learntrix.video.processing:mock}") String processingType,
            MockVideoProcessingService mockService,
            AwsMediaConvertVideoProcessingService awsService) {
        if ("aws".equalsIgnoreCase(processingType)) {
            return awsService;
        }
        return mockService;
    }
}
