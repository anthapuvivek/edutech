package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.recording.*;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.entity.Module;
import com.learntrix.edtech.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Statuses that constitute an active enrolment for access-control purposes.
// Kept as a package-private constant so it is easy to extend (e.g. "TRIAL")
// without hunting for every literal List.of("ACTIVE","ENROLLED") in the class.

@Service
@Transactional
public class ClassRecordingService {

    private static final List<String> ACTIVE_STATUSES = List.of("ACTIVE", "ENROLLED");

    private final ClassRecordingRepository recordingRepository;
    private final RecordingWatchProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final VideoStorageService videoStorageService;
    private final VideoProcessingService videoProcessingService;
    private final CourseAccessService courseAccessService;

    @Value("${video.completion-threshold:90}")
    private int completionThresholdPercent;

    public ClassRecordingService(
            ClassRecordingRepository recordingRepository,
            RecordingWatchProgressRepository progressRepository,
            CourseRepository courseRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            EnrollmentRepository enrollmentRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            VideoStorageService videoStorageService,
            VideoProcessingService videoProcessingService,
            CourseAccessService courseAccessService) {
        this.recordingRepository = recordingRepository;
        this.progressRepository = progressRepository;
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.videoStorageService = videoStorageService;
        this.videoProcessingService = videoProcessingService;
        this.courseAccessService = courseAccessService;
    }

    public RecordingResponse createRecording(CreateRecordingRequest request, UUID teacherId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        // A trainer may reach a course as its instructor, through a batch they teach, or
        // through students allocated to them - CourseAccessService is the single authority
        // on that. The old instructor-only check locked allocated trainers out of uploading.
        courseAccessService.verifyTeacherCanManageCourse(teacherId, course.getId());

        // The uploader owns the recording. Attributing it to course.getInstructor() meant an
        // allocated trainer failed every later ownership check on their own upload.
        User uploader = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        ClassRecording recording = new ClassRecording();
        recording.setCourse(course);
        recording.setTeacher(uploader);
        applyCurriculumMapping(recording, request.getModuleId(), request.getLessonId());
        recording.setTitle(request.getTitle());
        recording.setDescription(request.getDescription());
        recording.setClassDate(request.getClassDate() != null ? request.getClassDate() : Instant.now());
        recording.setStatus(RecordingStatus.DRAFT);
        recording.setPublished(false);

        ClassRecording saved = recordingRepository.save(recording);
        return mapToResponse(saved);
    }

    public RecordingResponse updateRecording(UUID id, UpdateRecordingRequest request, UUID teacherId) {
        ClassRecording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", id));

        if (!recording.getTeacher().getId().equals(teacherId)) {
            throw new CourseAccessDeniedException("You are not authorized to modify this recording");
        }

        recording.setTitle(request.getTitle());
        recording.setDescription(request.getDescription());
        if (request.getClassDate() != null) {
            recording.setClassDate(request.getClassDate());
        }

        // Only touch the curriculum mapping when the caller explicitly asked to, so an
        // ordinary title edit cannot wipe a mapping it never sent.
        if (request.isRemapCurriculum()) {
            applyCurriculumMapping(recording, request.getModuleId(), request.getLessonId());
        }

        recording.setUpdatedAt(Instant.now());

        ClassRecording saved = recordingRepository.save(recording);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public RecordingResponse getRecording(UUID id, UUID userId, boolean isAdmin, boolean isTeacher) {
        ClassRecording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", id));

        if (!isAdmin) {
            if (isTeacher) {
                if (!recording.getTeacher().getId().equals(userId)) {
                    throw new CourseAccessDeniedException("You do not have access to this recording");
                }
            } else {
                // Student check
                if (!recording.isPublished()) {
                    throw new CourseAccessDeniedException("Recording is not published yet");
                }
                boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatusIn(
                        userId, recording.getCourse().getId(), ACTIVE_STATUSES);
                if (!enrolled) {
                    throw new CourseAccessDeniedException("You must be enrolled in the course to view this recording");
                }
            }
        }

        return mapToResponse(recording);
    }

    @Transactional(readOnly = true)
    public Page<RecordingResponse> getTeacherRecordings(UUID teacherId, Pageable pageable) {
        return recordingRepository.findByTeacherId(teacherId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<RecordingResponse> getStudentRecordings(UUID studentId) {
        return recordingRepository.findPublishedByStudentEnrollment(studentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecordingResponse> getPublishedRecordingsForCourse(UUID courseId, UUID studentId) {
        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatusIn(
                studentId, courseId, ACTIVE_STATUSES);
        if (!enrolled) {
            throw new CourseAccessDeniedException("You must be enrolled in the course to view recordings");
        }
        return recordingRepository.findPublishedByCourseId(courseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RecordingResponse publishRecording(UUID id, UUID teacherId) {
        ClassRecording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", id));

        if (!recording.getTeacher().getId().equals(teacherId)) {
            throw new CourseAccessDeniedException("You are not authorized to publish this recording");
        }

        if (recording.getStatus() != RecordingStatus.READY && recording.getStatus() != RecordingStatus.UNPUBLISHED) {
            throw new BusinessException("INVALID_STATE", "Recording must be in READY or UNPUBLISHED state to be published", HttpStatus.BAD_REQUEST);
        }

        recording.setPublished(true);
        recording.setStatus(RecordingStatus.PUBLISHED);
        recording.setPublishedAt(Instant.now());
        recording.setUpdatedAt(Instant.now());

        ClassRecording saved = recordingRepository.save(recording);

        // Notify only the students who sit under this trainer for the course - their own
        // allocation, a batch they teach, or the course they instruct. On a course with
        // several trainers this stops one trainer's upload paging another's students.
        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getCourse().getId().equals(recording.getCourse().getId()) && "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .filter(e -> courseAccessService.isStudentAllocatedToTeacherCourse(
                        e.getStudent().getId(), teacherId, recording.getCourse().getId()))
                .toList();

        for (Enrollment enrollment : enrollments) {
            Notification notification = new Notification();
            notification.setUser(enrollment.getStudent());
            notification.setTitle("New recorded class available");
            notification.setMessage(String.format(
                    "Teacher %s has uploaded a new class recording: \"%s\" in your enrolled course \"%s\".",
                    recording.getTeacher().getName() != null ? recording.getTeacher().getName() : "Instructor",
                    recording.getTitle(),
                    recording.getCourse().getTitle()
            ));
            notificationRepository.save(notification);
        }

        return mapToResponse(saved);
    }

    public RecordingResponse unpublishRecording(UUID id, UUID teacherId) {
        ClassRecording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", id));

        if (!recording.getTeacher().getId().equals(teacherId)) {
            throw new CourseAccessDeniedException("You are not authorized to unpublish this recording");
        }

        recording.setPublished(false);
        recording.setStatus(RecordingStatus.UNPUBLISHED);
        recording.setUpdatedAt(Instant.now());

        ClassRecording saved = recordingRepository.save(recording);
        return mapToResponse(saved);
    }

    public void deleteRecording(UUID id, UUID teacherId) {
        ClassRecording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", id));

        if (!recording.getTeacher().getId().equals(teacherId)) {
            throw new CourseAccessDeniedException("You are not authorized to delete this recording");
        }

        // Delete from storage
        videoStorageService.deleteVideo(recording.getVideoStorageKey());
        
        // Delete HLS manifest path if processed
        if (recording.getVideoStorageKey() != null) {
            String hlsKey = recording.getVideoStorageKey().replace("/original/", "/processed/");
            videoStorageService.deleteVideo(hlsKey);
        }

        recordingRepository.delete(recording);
    }

    public UploadUrlResponse generateUploadUrl(UUID id, String fileName, UUID teacherId) {
        ClassRecording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", id));

        if (!recording.getTeacher().getId().equals(teacherId)) {
            throw new CourseAccessDeniedException("You are not authorized to upload videos for this recording");
        }

        UploadUrlResponse uploadInfo = videoStorageService.createUploadUrl(
                recording.getCourse().getId(),
                recording.getId(),
                fileName
        );

        recording.setStatus(RecordingStatus.UPLOADING);
        recording.setVideoStorageKey(uploadInfo.getStorageKey());
        recording.setUpdatedAt(Instant.now());
        recordingRepository.save(recording);

        return uploadInfo;
    }

    public RecordingResponse completeUpload(UUID id, UUID teacherId) {
        ClassRecording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", id));

        if (!recording.getTeacher().getId().equals(teacherId)) {
            throw new CourseAccessDeniedException("You are not authorized to manage this recording");
        }

        if (recording.getStatus() != RecordingStatus.UPLOADING) {
            throw new BusinessException("INVALID_STATE", "Upload cannot be marked complete unless status is UPLOADING", HttpStatus.BAD_REQUEST);
        }

        recording.setStatus(RecordingStatus.PROCESSING);
        recording.setUpdatedAt(Instant.now());
        ClassRecording saved = recordingRepository.save(recording);

        // Initiate video processing asynchronously
        videoProcessingService.startProcessing(saved.getId(), saved.getVideoStorageKey());

        return mapToResponse(saved);
    }

    public RecordingProgressResponse updateProgress(UUID recordingId, UUID studentId, RecordingProgressRequest request) {
        ClassRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", recordingId));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatusIn(
                studentId, recording.getCourse().getId(), ACTIVE_STATUSES);
        if (!enrolled) {
            throw new CourseAccessDeniedException("You must be enrolled in the course to track progress");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        RecordingWatchProgress progress = progressRepository.findByStudentIdAndRecordingId(studentId, recordingId)
                .orElseGet(() -> {
                    RecordingWatchProgress p = new RecordingWatchProgress();
                    p.setRecording(recording);
                    p.setStudent(student); // Use managed student user
                    return p;
                });

        progress.setWatchedSeconds(request.getWatchedSeconds());
        progress.setDurationSeconds(request.getDurationSeconds());
        progress.setLastWatchedAt(Instant.now());
        progress.setUpdatedAt(Instant.now());

        // Check completion threshold
        if (!progress.isCompleted() && progress.getDurationSeconds() > 0) {
            double percent = ((double) progress.getWatchedSeconds() / progress.getDurationSeconds()) * 100;
            if (percent >= completionThresholdPercent) {
                progress.setCompleted(true);
                progress.setCompletedAt(Instant.now());
            }
        }

        RecordingWatchProgress saved = progressRepository.save(progress);
        return RecordingProgressResponse.builder()
                .watchedSeconds(saved.getWatchedSeconds())
                .durationSeconds(saved.getDurationSeconds())
                .completed(saved.isCompleted())
                .lastWatchedAt(saved.getLastWatchedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public RecordingProgressResponse getProgress(UUID recordingId, UUID studentId) {
        ClassRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", recordingId));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatusIn(
                studentId, recording.getCourse().getId(), ACTIVE_STATUSES);
        if (!enrolled) {
            throw new CourseAccessDeniedException("You must be enrolled in the course to view progress");
        }

        return progressRepository.findByStudentIdAndRecordingId(studentId, recordingId)
                .map(p -> RecordingProgressResponse.builder()
                        .watchedSeconds(p.getWatchedSeconds())
                        .durationSeconds(p.getDurationSeconds())
                        .completed(p.isCompleted())
                        .lastWatchedAt(p.getLastWatchedAt())
                        .build())
                .orElse(RecordingProgressResponse.builder()
                        .watchedSeconds(0)
                        .durationSeconds(recording.getDurationSeconds() != null ? recording.getDurationSeconds() : 0)
                        .completed(false)
                        .lastWatchedAt(Instant.now())
                        .build());
    }

    @Transactional(readOnly = true)
    public RecordingStatisticsResponse getStatistics(UUID recordingId, UUID teacherId) {
        ClassRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassRecording", "id", recordingId));

        if (!recording.getTeacher().getId().equals(teacherId)) {
            throw new CourseAccessDeniedException("You are not authorized to view statistics for this recording");
        }

        // Count enrollments for course
        long totalEnrolled = enrollmentRepository.findAll().stream()
                .filter(e -> e.getCourse().getId().equals(recording.getCourse().getId()) && "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .count();

        // Get progress for this recording
        List<RecordingWatchProgress> progressList = progressRepository.findAll().stream()
                .filter(p -> p.getRecording().getId().equals(recordingId))
                .toList();

        int started = 0;
        int completed = 0;
        double totalWatchDuration = 0;
        double totalWatchPercent = 0;

        for (RecordingWatchProgress progress : progressList) {
            if (progress.getWatchedSeconds() > 0) {
                started++;
            }
            if (progress.isCompleted()) {
                completed++;
            }
            totalWatchDuration += progress.getWatchedSeconds();
            if (progress.getDurationSeconds() > 0) {
                totalWatchPercent += ((double) progress.getWatchedSeconds() / progress.getDurationSeconds()) * 100;
            }
        }

        double avgPercentage = started > 0 ? (totalWatchPercent / started) : 0;
        double avgDuration = started > 0 ? (totalWatchDuration / started) : 0;
        double completionRate = started > 0 ? (((double) completed / started) * 100) : 0;

        return RecordingStatisticsResponse.builder()
                .recordingId(recordingId)
                .totalStudents((int) totalEnrolled)
                .studentsStarted(started)
                .studentsCompleted(completed)
                .averageWatchPercentage(Math.round(avgPercentage * 10.0) / 10.0)
                .averageWatchDuration(Math.round(avgDuration * 10.0) / 10.0)
                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<RecordingResponse> getAllRecordings(Pageable pageable) {
        return recordingRepository.findAll(pageable).map(this::mapToResponse);
    }

    /**
     * Resolves the optional module/lesson mapping onto a recording.
     *
     * <p>Both ids may be null: a trainer can upload a class before the course curriculum
     * has been built, and the recording is then filed against the course alone. Whatever
     * IS supplied is still verified - the module must belong to the recording's course and
     * the lesson to that module - and a lesson supplied on its own adopts its own module.
     */
    private void applyCurriculumMapping(ClassRecording recording, UUID moduleId, UUID lessonId) {
        Lesson lesson = lessonId == null ? null
                : lessonRepository.findById(lessonId)
                        .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        Module module = moduleId == null ? null
                : moduleRepository.findById(moduleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        if (module == null && lesson != null) {
            module = lesson.getModule();
        }

        if (module != null && !module.getCourse().getId().equals(recording.getCourse().getId())) {
            throw new BusinessException("INVALID_RELATIONSHIP",
                    "Module does not belong to the selected course", HttpStatus.BAD_REQUEST);
        }

        if (lesson != null && module != null && !lesson.getModule().getId().equals(module.getId())) {
            throw new BusinessException("INVALID_RELATIONSHIP",
                    "Lesson does not belong to the selected module", HttpStatus.BAD_REQUEST);
        }

        recording.setModule(module);
        recording.setLesson(lesson);
    }

    private RecordingResponse mapToResponse(ClassRecording recording) {
        return RecordingResponse.builder()
                .id(recording.getId())
                .courseId(recording.getCourse().getId())
                .courseTitle(recording.getCourse().getTitle())
                // Null whenever the trainer uploaded without mapping the recording to the
                // curriculum. Clients render a fallback label rather than assuming a mapping.
                .moduleId(recording.getModule() != null ? recording.getModule().getId() : null)
                .moduleTitle(recording.getModule() != null ? recording.getModule().getTitle() : null)
                .lessonId(recording.getLesson() != null ? recording.getLesson().getId() : null)
                .lessonTitle(recording.getLesson() != null ? recording.getLesson().getTitle() : null)
                .teacherId(recording.getTeacher().getId())
                .teacherName(recording.getTeacher().getName())
                .title(recording.getTitle())
                .description(recording.getDescription())
                .classDate(recording.getClassDate())
                .videoStorageKey(recording.getVideoStorageKey())
                .videoUrl(recording.getVideoUrl())
                .hlsManifestUrl(recording.getHlsManifestUrl())
                .thumbnailUrl(recording.getThumbnailUrl())
                .durationSeconds(recording.getDurationSeconds())
                .fileSizeBytes(recording.getFileSizeBytes())
                .videoFormat(recording.getVideoFormat())
                .status(recording.getStatus().name())
                .published(recording.isPublished())
                .createdAt(recording.getCreatedAt())
                .publishedAt(recording.getPublishedAt())
                .build();
    }
}
