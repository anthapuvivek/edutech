package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.recording.RecordingProgressRequest;
import com.learntrix.edtech.dto.recording.RecordingProgressResponse;
import com.learntrix.edtech.dto.recording.RecordingResponse;
import com.learntrix.edtech.service.ClassRecordingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/recordings")
@PreAuthorize("hasRole('STUDENT')")
public class StudentRecordingController {

    private final ClassRecordingService recordingService;

    public StudentRecordingController(ClassRecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @GetMapping
    public ApiResponse<List<RecordingResponse>> getStudentRecordings() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<RecordingResponse> response = recordingService.getStudentRecordings(studentId);
        return ApiResponse.success(response);
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<List<RecordingResponse>> getPublishedRecordingsForCourse(@PathVariable("courseId") UUID courseId) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<RecordingResponse> response = recordingService.getPublishedRecordingsForCourse(courseId, studentId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<RecordingResponse> getRecording(@PathVariable("id") UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        RecordingResponse response = recordingService.getRecording(id, studentId, false, false);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}/progress")
    public ApiResponse<RecordingProgressResponse> getProgress(@PathVariable("id") UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        RecordingProgressResponse response = recordingService.getProgress(id, studentId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/progress")
    public ApiResponse<RecordingProgressResponse> updateProgress(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecordingProgressRequest request) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        RecordingProgressResponse response = recordingService.updateProgress(id, studentId, request);
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<RecordingProgressResponse> completeRecording(@PathVariable("id") UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        // Force complete: update progress with watchedSeconds = durationSeconds
        RecordingProgressResponse current = recordingService.getProgress(id, studentId);
        int duration = current.getDurationSeconds() > 0 ? current.getDurationSeconds() : 3600; // fallback default
        
        RecordingProgressRequest request = new RecordingProgressRequest();
        request.setWatchedSeconds(duration);
        request.setDurationSeconds(duration);
        
        RecordingProgressResponse response = recordingService.updateProgress(id, studentId, request);
        return ApiResponse.success(response);
    }
}
