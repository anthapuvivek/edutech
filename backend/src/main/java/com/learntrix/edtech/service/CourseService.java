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
     * Public catalogue query.
     * Enforces that only PUBLISHED courses are visible to students and visitors on the public storefront.
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
                .filter(c -> "PUBLISHED".equalsIgnoreCase(c.getStatus()))
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

    /**
     * Admin course query — returns all courses across all statuses (DRAFT, PUBLISHED, ARCHIVED).
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> listAdminCourses(String search, String category, String status, String sort) {
        List<Course> courses = courseRepository.findAll();

        return courses.stream()
                .filter(c -> {
                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.toLowerCase();
                        return c.getTitle().toLowerCase().contains(s) ||
                               (c.getSubtitle() != null && c.getSubtitle().toLowerCase().contains(s)) ||
                               (c.getSlug() != null && c.getSlug().toLowerCase().contains(s)) ||
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
                .filter(c -> {
                    if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("All")) {
                        return status.equalsIgnoreCase(c.getStatus());
                    }
                    return true;
                })
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

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
        return getCourseBySlug(course.getSlug());
    }

    /**
     * Admin creates a new Course entity and saves it to PostgreSQL.
     */
    public CourseResponse createCourse(CreateCourseRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Course title is required");
        }
        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Course category is required");
        }

        String slug = generateUniqueSlug(request.getTitle(), request.getSlug());

        Course course = new Course();
        course.setTitle(request.getTitle().trim());
        course.setSlug(slug);
        course.setSubtitle(request.getSubtitle() != null ? request.getSubtitle().trim() : null);
        course.setCategory(request.getCategory().trim());
        course.setThumbnailUrl(request.getThumbnailUrl() != null && !request.getThumbnailUrl().trim().isEmpty()
                ? request.getThumbnailUrl().trim()
                : "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70");
        course.setLevel(request.getLevel() != null && !request.getLevel().trim().isEmpty() ? request.getLevel().trim() : "Intermediate");
        course.setLanguage(request.getLanguage() != null && !request.getLanguage().trim().isEmpty() ? request.getLanguage().trim() : "English");
        course.setDurationHours(request.getDurationHours() != null ? request.getDurationHours() : 36);
        course.setLessonCount(request.getLessonCount() != null ? request.getLessonCount() : 12);
        course.setPrice(request.getPrice() != null ? request.getPrice() : 0);
        course.setOriginalPrice(request.getOriginalPrice() != null ? request.getOriginalPrice() : (request.getPrice() != null ? (int)(request.getPrice() * 1.5) : 0));
        course.setCurrency(request.getCurrency() != null && !request.getCurrency().trim().isEmpty() ? request.getCurrency().trim() : "INR");
        course.setStatus(request.getStatus() != null && !request.getStatus().trim().isEmpty() ? request.getStatus().trim().toUpperCase() : "DRAFT");
        course.setRating(5.0);
        course.setRatingCount(1);
        course.setStudentCount(0);

        if (request.getInstructorId() != null) {
            userRepository.findById(request.getInstructorId()).ifPresent(course::setInstructor);
        }

        Course saved = courseRepository.save(course);
        return mapToCourseResponse(saved);
    }

    /**
     * Admin updates an existing Course entity.
     */
    public CourseResponse updateCourse(UUID courseId, UpdateCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            course.setTitle(request.getTitle().trim());
        }
        if (request.getSlug() != null && !request.getSlug().trim().isEmpty() && !request.getSlug().trim().equalsIgnoreCase(course.getSlug())) {
            course.setSlug(generateUniqueSlug(course.getTitle(), request.getSlug()));
        }
        if (request.getSubtitle() != null) {
            course.setSubtitle(request.getSubtitle().trim());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            course.setCategory(request.getCategory().trim());
        }
        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().trim().isEmpty()) {
            course.setThumbnailUrl(request.getThumbnailUrl().trim());
        }
        if (request.getLevel() != null && !request.getLevel().trim().isEmpty()) {
            course.setLevel(request.getLevel().trim());
        }
        if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
            course.setLanguage(request.getLanguage().trim());
        }
        if (request.getDurationHours() != null) {
            course.setDurationHours(request.getDurationHours());
        }
        if (request.getLessonCount() != null) {
            course.setLessonCount(request.getLessonCount());
        }
        if (request.getPrice() != null) {
            course.setPrice(request.getPrice());
        }
        if (request.getOriginalPrice() != null) {
            course.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
            course.setCurrency(request.getCurrency().trim());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            course.setStatus(request.getStatus().trim().toUpperCase());
        }
        if (request.getInstructorId() != null) {
            userRepository.findById(request.getInstructorId()).ifPresent(course::setInstructor);
        }

        Course saved = courseRepository.save(course);
        return mapToCourseResponse(saved);
    }

    /**
     * Admin publishes a course: sets status = PUBLISHED and persists to PostgreSQL.
     */
    public CourseResponse publishCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        course.setStatus("PUBLISHED");
        Course saved = courseRepository.save(course);
        return mapToCourseResponse(saved);
    }

    /**
     * Admin unpublishes a course: sets status = DRAFT and persists to PostgreSQL.
     */
    public CourseResponse unpublishCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        course.setStatus("DRAFT");
        Course saved = courseRepository.save(course);
        return mapToCourseResponse(saved);
    }

    /**
     * Admin updates course status (DRAFT, PUBLISHED, ARCHIVED).
     */
    public CourseResponse setCourseStatus(UUID courseId, String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Course status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid course status: " + status + ". Allowed: DRAFT, PUBLISHED, ARCHIVED");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        course.setStatus(normalized);
        Course saved = courseRepository.save(course);
        return mapToCourseResponse(saved);
    }

    /**
     * Admin deletes or archives a course.
     */
    public void deleteCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        
        boolean hasEnrollments = enrollmentRepository.findByCourseId(courseId).stream().findAny().isPresent();
        if (hasEnrollments) {
            course.setStatus("ARCHIVED");
            courseRepository.save(course);
        } else {
            courseRepository.delete(course);
        }
    }

    /**
     * Student course enrollment.
     * Verifies:
     *  1. Course exists.
     *  2. Course is PUBLISHED (cannot enroll in draft/archived courses).
     *  3. Student is not already enrolled (prevents duplicates with 409 Conflict).
     *  4. Persists enrollment to PostgreSQL.
     *  5. Updates course studentCount.
     */
    public EnrollmentResponse enrollStudent(UUID courseId, UUID studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        if (!"PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            throw new ConflictException("COURSE_NOT_PUBLISHED", "Cannot enroll in a course that is not currently published.");
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("ENROLLMENT_ALREADY_EXISTS", "You are already enrolled in this course.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollment.setStatus("ACTIVE");

        Enrollment saved = enrollmentRepository.save(enrollment);

        // Increment studentCount on course
        course.setStudentCount(course.getStudentCount() != null ? course.getStudentCount() + 1 : 1);
        courseRepository.save(course);

        return mapToEnrollmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getStudentEnrollments(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        return enrollments.stream()
                .map(this::mapToEnrollmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Reusable authorization check: determines whether the student has active access to the given course.
     */
    @Transactional(readOnly = true)
    public boolean hasStudentCourseAccess(UUID studentId, UUID courseId) {
        if (studentId == null || courseId == null) {
            return false;
        }
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Get access status for a student on a specific course.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> checkCourseAccess(UUID courseId, UUID studentId) {
        boolean hasAccess = hasStudentCourseAccess(studentId, courseId);
        return java.util.Map.of(
                "courseId", courseId,
                "hasAccess", hasAccess,
                "status", hasAccess ? "ACTIVE" : "NOT_ENROLLED"
        );
    }

    private String generateUniqueSlug(String title, String requestedSlug) {
        String base = (requestedSlug != null && !requestedSlug.trim().isEmpty())
                ? requestedSlug.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                : title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "course-" + System.currentTimeMillis();
        String candidate = base;
        int counter = 1;
        while (courseRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + counter++;
        }
        return candidate;
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
        if (course.getSlug() != null) {
            if (course.getSlug().contains("python")) {
                skills = List.of("Python", "FastAPI", "Pandas", "OOP");
            } else if (course.getSlug().contains("react") || course.getSlug().contains("full-stack")) {
                skills = List.of("React", "TypeScript", "TailwindCSS", "Node.js");
            } else if (course.getSlug().contains("devops") || course.getSlug().contains("cloud")) {
                skills = List.of("Docker", "Kubernetes", "AWS", "CI/CD");
            } else if (course.getSlug().contains("data") || course.getSlug().contains("ai")) {
                skills = List.of("Python", "Machine Learning", "PyTorch", "Data Analysis");
            }
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
                .status(course.getStatus() != null ? course.getStatus() : "PUBLISHED")
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
            totalLessons = course.getLessonCount() != null && course.getLessonCount() > 0 ? course.getLessonCount() : 10;
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

