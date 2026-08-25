package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ConflictException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.course.*;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.entity.Module;
import com.learntrix.edtech.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final RecordingWatchProgressRepository progressRepository;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            RecordingWatchProgressRepository progressRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses(String search, String category) {
        return listCourses(search, category, null, null, null, null);
    }

    /**
     * Catalogue query. Every parameter mirrors CourseQuery in src/types/index.ts; the
     * storefront's filter sidebar sends all of them, so all of them are applied here
     * rather than being silently dropped.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses(String search,
                                            String category,
                                            String level,
                                            Integer maxPrice,
                                            Double minRating,
                                            String sort) {
        List<Course> courses = courseRepository.findAll();

        return courses.stream()
                .filter(c -> {
                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.toLowerCase();
                        return c.getTitle().toLowerCase().contains(s) ||
                               (c.getSubtitle() != null && c.getSubtitle().toLowerCase().contains(s)) ||
                               c.getCategory().toLowerCase().contains(s);
                    }
                    return true;
                })
                .filter(c -> {
                    if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
                        return c.getCategory().equalsIgnoreCase(category);
                    }
                    return true;
                })
                .filter(c -> isAnyOption(level) || level.equalsIgnoreCase(c.getLevel()))
                .filter(c -> maxPrice == null || c.getPrice() <= maxPrice)
                .filter(c -> minRating == null || c.getRating() >= minRating)
                .sorted(courseComparator(sort))
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());
    }

    /** Treats a missing filter and the sidebar's explicit "All" option the same way. */
    private static boolean isAnyOption(String filter) {
        return filter == null || filter.isBlank() || "All".equalsIgnoreCase(filter);
    }

    private static Comparator<Course> courseComparator(String sort) {
        if (sort == null) return Comparator.comparing(Course::getTitle);
        return switch (sort.toLowerCase()) {
            case "rating" -> Comparator.comparing(Course::getRating).reversed();
            case "price_low" -> Comparator.comparing(Course::getPrice);
            case "price_high" -> Comparator.comparing(Course::getPrice).reversed();
            case "newest" -> Comparator.comparing(
                    Course::getUpdatedAt, Comparator.nullsLast(Comparator.<Instant>naturalOrder())).reversed();
            case "popular" -> Comparator.comparing(Course::getStudentCount).reversed();
            default -> Comparator.comparing(Course::getTitle);
        };
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "slug", slug));

        CourseResponse response = mapToCourseResponse(course);
        
        // Populate modules & lessons detail
        List<Module> modules = moduleRepository.findByCourseIdOrderBySequenceNumberAsc(course.getId());
        List<ModuleResponse> moduleResponses = modules.stream().map(m -> {
            List<Lesson> lessons = lessonRepository.findByModuleIdOrderBySequenceNumberAsc(m.getId());
            List<LessonResponse> lessonResponses = lessons.stream().map(l -> 
                LessonResponse.builder()
                        .id(l.getId())
                        .title(l.getTitle())
                        .sequenceNumber(l.getSequenceNumber())
                        .durationSeconds(l.getDurationSeconds())
                        .build()
            ).collect(Collectors.toList());

            return ModuleResponse.builder()
                    .id(m.getId())
                    .title(m.getTitle())
                    .sequenceNumber(m.getSequenceNumber())
                    .lessons(lessonResponses)
                    .build();
        }).collect(Collectors.toList());

        response.setModules(moduleResponses);
        return response;
    }

    public EnrollmentResponse enrollStudent(UUID courseId, UUID studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("ENROLLMENT_ALREADY_EXISTS", "Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollment.setStatus("ACTIVE");

        Enrollment saved = enrollmentRepository.save(enrollment);
        return mapToEnrollmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getStudentEnrollments(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        return enrollments.stream()
                .map(this::mapToEnrollmentResponse)
                .collect(Collectors.toList());
    }

    private CourseResponse mapToCourseResponse(Course course) {
        InstructorResponse instructorResponse = null;
        if (course.getInstructor() != null) {
            User inst = course.getInstructor();
            instructorResponse = InstructorResponse.builder()
                    .id(inst.getId())
                    .name(inst.getName())
                    .title("Lead Instructor & Solutions Architect")
                    .avatarUrl(inst.getAvatarUrl())
                    .bio("Expert systems designer and developer. Teaching deep dive technical architectures.")
                    .rating(4.9)
                    .students(18400)
                    .build();
        }

        // Skills are still derived: courses has no skills column yet.
        List<String> skills = List.of("Java", "Spring Boot", "REST APIs", "SQL");
        if (course.getSlug().contains("python")) {
            skills = List.of("Python", "FastAPI", "Pandas", "OOP");
        } else if (course.getSlug().contains("react") || course.getSlug().contains("full-stack")) {
            skills = List.of("React", "TypeScript", "TailwindCSS", "Node.js");
        }

        return CourseResponse.builder()
                .id(course.getId())
                .slug(course.getSlug())
                .title(course.getTitle())
                .subtitle(course.getSubtitle())
                .category(course.getCategory())
                .skills(skills)
                .level(course.getLevel())
                .language(course.getLanguage())
                .durationHours(course.getDurationHours())
                .lessonCount(course.getLessonCount())
                .rating(course.getRating())
                .ratingCount(course.getRatingCount())
                .studentCount(course.getStudentCount())
                .price(course.getPrice())
                .originalPrice(course.getOriginalPrice())
                .currency(course.getCurrency())
                .thumbnailUrl(course.getThumbnailUrl())
                .instructor(instructorResponse)
                .badges(List.of("Career Track"))
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private EnrollmentResponse mapToEnrollmentResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        String instructorName = course.getInstructor() != null ? course.getInstructor().getName() : "Instructor";
        
        // Calculate progress completed based on recordings watch progress
        List<Module> modules = moduleRepository.findByCourseIdOrderBySequenceNumberAsc(course.getId());
        int totalLessons = 0;
        int completedLessons = 0;
        
        for (Module m : modules) {
            List<Lesson> lessons = lessonRepository.findByModuleIdOrderBySequenceNumberAsc(m.getId());
            totalLessons += lessons.size();
            for (Lesson l : lessons) {
                // If student has watched recording associated with this lesson and marked as completed
                // Let's check watch progress for this student & lesson recording
                boolean isCompleted = progressRepository.findAll().stream()
                        .filter(p -> p.getStudent().getId().equals(enrollment.getStudent().getId())
                                && p.getRecording().getLesson().getId().equals(l.getId()))
                        .anyMatch(RecordingWatchProgress::isCompleted);
                if (isCompleted) {
                    completedLessons++;
                }
            }
        }

        if (totalLessons == 0) {
            totalLessons = 10; // Avoid divide by 0 and give default
        }
        int progressPercent = (int) (((double) completedLessons / totalLessons) * 100);

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(course.getId())
                .courseSlug(course.getSlug())
                .courseTitle(course.getTitle())
                .instructorName(instructorName)
                .thumbnailUrl(course.getThumbnailUrl())
                .status(enrollment.getStatus().toLowerCase())
                .progressPercent(progressPercent)
                .lessonsCompleted(completedLessons)
                .lessonsTotal(totalLessons)
                .lastLessonTitle("Module 1 · Setup & Installation")
                .nextLessonTitle("Module 2 · Configuration")
                .enrolledAt(enrollment.getCreatedAt())
                .build();
    }
}
