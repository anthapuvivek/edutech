package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.recording.*;
import com.learntrix.edtech.service.ClassRecordingService;
import com.learntrix.edtech.storage.LocalVideoStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/recordings")
@PreAuthorize("hasRole('TEACHER')")
public class ClassRecordingController {

    private final ClassRecordingService recordingService;
    private final LocalVideoStorageService localVideoStorageService;

    public ClassRecordingController(
            ClassRecordingService recordingService,
            LocalVideoStorageService localVideoStorageService) {
        this.recordingService = recordingService;
        this.localVideoStorageService = localVideoStorageService;
    }

    @PostMapping
    public ApiResponse<RecordingResponse> createRecording(@Valid @RequestBody CreateRecordingRequest request) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        RecordingResponse response = recordingService.createRecording(request, teacherId);
        return ApiResponse.success(response);
    }

    @GetMapping("/teacher")
    public ApiResponse<Page<RecordingResponse>> getTeacherRecordings(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RecordingResponse> response = recordingService.getTeacherRecordings(teacherId, pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<RecordingResponse> getRecording(@PathVariable("id") UUID id) {
        UUID userId = SecurityUtil.getCurrentUserId();
        RecordingResponse response = recordingService.getRecording(id, userId, false, true);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<RecordingResponse> updateRecording(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRecordingRequest request) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        RecordingResponse response = recordingService.updateRecording(id, request, teacherId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRecording(@PathVariable("id") UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        recordingService.deleteRecording(id, teacherId);
        return ApiResponse.success("Recording deleted successfully");
    }

    @PostMapping("/{id}/upload")
    public ApiResponse<UploadUrlResponse> generateUploadUrl(
            @PathVariable("id") UUID id,
            @RequestParam("fileName") String fileName) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        UploadUrlResponse response = recordingService.generateUploadUrl(id, fileName, teacherId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/upload-complete")
    public ApiResponse<RecordingResponse> completeUpload(@PathVariable("id") UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        RecordingResponse response = recordingService.completeUpload(id, teacherId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<RecordingResponse> publishRecording(@PathVariable("id") UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        RecordingResponse response = recordingService.publishRecording(id, teacherId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<RecordingResponse> unpublishRecording(@PathVariable("id") UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        RecordingResponse response = recordingService.unpublishRecording(id, teacherId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}/statistics")
    public ApiResponse<RecordingStatisticsResponse> getStatistics(@PathVariable("id") UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        RecordingStatisticsResponse response = recordingService.getStatistics(id, teacherId);
        return ApiResponse.success(response);
    }

    /** Endpoint to receive local file upload payloads during development */
    @PostMapping(value = "/upload-local", consumes = "multipart/form-data")
    @PreAuthorize("permitAll()")
    public ApiResponse<String> uploadLocalMultipart(
            @RequestParam("key") String key,
            @RequestParam("file") MultipartFile file) throws IOException {
        localVideoStorageService.storeFile(key, file.getBytes());
        return ApiResponse.success("File uploaded successfully");
    }

    @PutMapping(value = "/upload-local")
    @PreAuthorize("permitAll()")
    public ApiResponse<String> uploadLocalPut(
            @RequestParam("key") String key,
            HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readAllBytes();
        localVideoStorageService.storeFile(key, bytes);
        return ApiResponse.success("File uploaded successfully");
    }
}
