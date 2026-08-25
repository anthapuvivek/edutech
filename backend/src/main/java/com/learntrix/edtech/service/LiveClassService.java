package com.learntrix.edtech.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.live.LiveClassResponse;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.LiveClass;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.LiveClassRepository;
import com.learntrix.edtech.repository.UserRepository;

@Service
@Transactional
public class LiveClassService {

    private final LiveClassRepository liveClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    public LiveClassService(
            LiveClassRepository liveClassRepository,
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            BatchRepository batchRepository,
            UserRepository userRepository) {
        this.liveClassRepository = liveClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LiveClassResponse> getStudentLiveClasses(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<UUID> enrolledCourseIds = enrollments.stream()
                .filter(e -> "active".equalsIgnoreCase(e.getStatus()) || "enrolled".equalsIgnoreCase(e.getStatus()))
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toList());

        if (enrolledCourseIds.isEmpty()) {
            enrolledCourseIds = List.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        }

        List<LiveClass> liveClasses = liveClassRepository.findAvailableLiveClasses(studentId, enrolledCourseIds);
        return liveClasses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LiveClassResponse> getTeacherLiveClasses(UUID teacherId) {
        return liveClassRepository.findByTeacherOrBatchTeacher(teacherId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LiveClassResponse createLiveClass(Map<String, Object> body, UUID teacherId) {
        LiveClass liveClass = new LiveClass();
        liveClass.setId(UUID.randomUUID());

        if (body.containsKey("title")) {
            liveClass.setTitle((String) body.get("title"));
        }

        if (body.containsKey("courseId")) {
            UUID courseId = UUID.fromString((String) body.get("courseId"));
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
            liveClass.setCourse(course);
        }

        if (body.containsKey("batchId")) {
            UUID batchId = UUID.fromString((String) body.get("batchId"));
            Batch batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
            if (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId)) {
                throw new ResourceNotFoundException("Batch", "teacher", teacherId);
            }
            liveClass.setBatch(batch);
            liveClass.setTeacher(batch.getTeacher());
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));
        liveClass.setTeacher(teacher);

        if (body.containsKey("trainerName")) {
            liveClass.setTrainerName((String) body.get("trainerName"));
        } else {
            liveClass.setTrainerName(teacher.getName());
        }
        
        if (body.containsKey("description")) {
            liveClass.setDescription((String) body.get("description"));
        }
        
        if (body.containsKey("classDate")) {
            liveClass.setClassDate(LocalDate.parse((String) body.get("classDate")));
        } else {
            liveClass.setClassDate(LocalDate.now().plusDays(1));
        }
        
        if (body.containsKey("startTime")) {
            liveClass.setStartTime(LocalTime.parse((String) body.get("startTime")));
        } else {
            liveClass.setStartTime(LocalTime.of(9, 0));
        }
        
        if (body.containsKey("endTime")) {
            liveClass.setEndTime(LocalTime.parse((String) body.get("endTime")));
        } else {
            liveClass.setEndTime(LocalTime.of(10, 30));
        }
        
        if (body.containsKey("platform")) {
            liveClass.setPlatform((String) body.get("platform"));
        } else {
            liveClass.setPlatform("Google Meet");
        }
        
        if (body.containsKey("meetingUrl")) {
            liveClass.setMeetingUrl((String) body.get("meetingUrl"));
        }
        
        if (body.containsKey("type")) {
            liveClass.setType((String) body.get("type"));
        } else {
            liveClass.setType("Live Class");
        }
        
        if (body.containsKey("visibility")) {
            liveClass.setVisibility((String) body.get("visibility"));
        } else {
            liveClass.setVisibility("restricted");
        }
        
        if (body.containsKey("published")) {
            liveClass.setPublished((Boolean) body.get("published"));
        } else {
            liveClass.setPublished(true);
        }
        
        if (body.containsKey("status")) {
            liveClass.setStatus((String) body.get("status"));
        } else {
            liveClass.setStatus("Upcoming");
        }
        
        if (body.containsKey("registrations")) {
            liveClass.setRegistrations((Integer) body.get("registrations"));
        } else {
            liveClass.setRegistrations(0);
        }
        
        LiveClass saved = liveClassRepository.save(liveClass);
        return mapToResponse(saved);
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
                .batchId(lc.getBatch() != null ? lc.getBatch().getId() : null)
                .batchName(lc.getBatch() != null ? lc.getBatch().getName() : null)
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
