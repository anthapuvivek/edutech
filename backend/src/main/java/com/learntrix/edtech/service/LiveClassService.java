package com.learntrix.edtech.service;

import com.learntrix.edtech.dto.live.LiveClassResponse;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.LiveClass;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.LiveClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LiveClassService {

    private final LiveClassRepository liveClassRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LiveClassService(
            LiveClassRepository liveClassRepository,
            EnrollmentRepository enrollmentRepository) {
        this.liveClassRepository = liveClassRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<LiveClassResponse> getStudentLiveClasses(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<UUID> enrolledCourseIds = enrollments.stream()
                .filter(e -> "active".equalsIgnoreCase(e.getStatus()))
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toList());

        List<LiveClass> liveClasses;
        if (enrolledCourseIds.isEmpty()) {
            liveClasses = liveClassRepository.findPublicLiveClasses();
        } else {
            liveClasses = liveClassRepository.findAvailableLiveClasses(enrolledCourseIds);
        }

        return liveClasses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LiveClassResponse mapToResponse(LiveClass lc) {
        String startTimeStr = lc.getStartTime() != null ? lc.getStartTime().toString() : "";
        if (startTimeStr.length() > 5) {
            startTimeStr = startTimeStr.substring(0, 5);
        }

        String endTimeStr = lc.getEndTime() != null ? lc.getEndTime().toString() : "";
        if (endTimeStr.length() > 5) {
            endTimeStr = endTimeStr.substring(0, 5);
        }

        return LiveClassResponse.builder()
                .id(lc.getId())
                .title(lc.getTitle())
                .courseTitle(lc.getCourse() != null ? lc.getCourse().getTitle() : "General Platform Event")
                .trainerName(lc.getTrainerName())
                .description(lc.getDescription())
                .date(lc.getClassDate() != null ? lc.getClassDate().toString() : "")
                .startTime(startTimeStr)
                .endTime(endTimeStr)
                .platform(lc.getPlatform())
                .meetingUrl(lc.getMeetingUrl())
                .type(lc.getType())
                .visibility(lc.getVisibility())
                .published(lc.getPublished())
                .status(lc.getStatus())
                .registrations(lc.getRegistrations())
                .build();
    }
}
