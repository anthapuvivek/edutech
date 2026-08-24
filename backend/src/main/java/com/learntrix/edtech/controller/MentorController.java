package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.mentor.*;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MentorController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MentoringSessionRepository mentoringSessionRepository;
    private final MentoringNoteRepository mentoringNoteRepository;

    public MentorController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            EnrollmentRepository enrollmentRepository,
            MentoringSessionRepository mentoringSessionRepository,
            MentoringNoteRepository mentoringNoteRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.mentoringSessionRepository = mentoringSessionRepository;
        this.mentoringNoteRepository = mentoringNoteRepository;
    }

    @GetMapping("/mentors")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'MENTOR')")
    public ApiResponse<List<MentorResponse>> getMentors() {
        List<User> mentors = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "MENTOR".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        List<MentorResponse> responses = mentors.stream()
                .map(m -> MentorResponse.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .email(m.getEmail())
                        .avatarUrl(m.getAvatarUrl())
                        .phone("+91 99999 88888")
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/mentor/stats")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<MentorStatsResponse> getStats() {
        UUID mentorId = SecurityUtil.getCurrentUserId();
        int studentCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .count();

        int sessionsCount = (int) mentoringSessionRepository.findByMentorId(mentorId).stream()
                .filter(s -> s.getSessionDate().isAfter(LocalDate.now().minusDays(1)))
                .count();

        return ApiResponse.success(MentorStatsResponse.builder()
                .totalStudents(studentCount)
                .activeStudents(studentCount)
                .upcomingSessions(sessionsCount)
                .pendingTasks(1)
                .studentsAtRisk(0)
                .averageCareerReadiness(78.5)
                .averageProgress(72.0)
                .build());
    }

    @GetMapping("/mentor/students")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<List<MentorStudentResponse>> getStudents() {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        List<MentorStudentResponse> responses = students.stream()
                .map(this::mapToMentorStudentResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @GetMapping("/mentor/sessions")
    @PreAuthorize("hasAnyRole('MENTOR', 'STUDENT')")
    public ApiResponse<List<MentorSessionResponse>> getSessions() {
        UUID userId = SecurityUtil.getCurrentUserId();
        // Return sessions where user is either mentor or student
        List<MentoringSession> sessions = mentoringSessionRepository.findAll().stream()
                .filter(s -> s.getMentor().getId().equals(userId) || s.getStudent().getId().equals(userId))
                .collect(Collectors.toList());

        List<MentorSessionResponse> responses = sessions.stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @PostMapping("/mentor/sessions")
    @PreAuthorize("hasAnyRole('MENTOR', 'STUDENT')")
    public ApiResponse<MentorSessionResponse> createSession(@RequestBody Map<String, Object> body) {
        UUID studentId = UUID.fromString((String) body.get("studentId"));
        User student = userRepository.findById(studentId).orElse(null);

        UUID mentorId = SecurityUtil.getCurrentUserId();
        User mentor = userRepository.findById(mentorId).orElse(null);

        if (student != null && mentor != null) {
            MentoringSession session = new MentoringSession();
            session.setId(UUID.randomUUID());
            session.setMentor(mentor);
            session.setStudent(student);
            session.setTitle(body.containsKey("title") ? (String) body.get("title") : "Mentoring Session");
            session.setKind(body.containsKey("kind") ? (String) body.get("kind") : "Mentoring");
            session.setSessionTime((String) body.get("time"));
            session.setPlatform((String) body.get("platform"));
            session.setMeetingUrl((String) body.get("meetingUrl"));
            session.setAgenda((String) body.get("agenda"));
            session.setNotes((String) body.get("notes"));
            session.setStatus("Scheduled");

            if (body.containsKey("date")) {
                session.setSessionDate(LocalDate.parse((String) body.get("date")));
            }
            if (body.containsKey("durationMinutes")) {
                session.setDurationMinutes((Integer) body.get("durationMinutes"));
            }

            MentoringSession saved = mentoringSessionRepository.save(session);
            return ApiResponse.success(mapToSessionResponse(saved));
        }

        return ApiResponse.success(null);
    }

    @PatchMapping("/mentor/sessions/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'STUDENT')")
    public ApiResponse<Map<String, Object>> updateSession(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        MentoringSession session = mentoringSessionRepository.findById(id).orElse(null);
        if (session != null) {
            if (body.containsKey("title")) {
                session.setTitle((String) body.get("title"));
            }
            if (body.containsKey("status")) {
                session.setStatus((String) body.get("status"));
            }
            if (body.containsKey("notes")) {
                session.setNotes((String) body.get("notes"));
            }
            mentoringSessionRepository.save(session);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/mentor/notes")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<List<MentorNoteResponse>> getNotes() {
        UUID mentorId = SecurityUtil.getCurrentUserId();
        List<MentoringNote> notes = mentoringNoteRepository.findByMentorId(mentorId);
        List<MentorNoteResponse> responses = notes.stream()
                .map(this::mapToNoteResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping("/mentor/notes")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<Map<String, Object>> createNote(@RequestBody Map<String, Object> body) {
        UUID studentId = UUID.fromString((String) body.get("studentId"));
        User student = userRepository.findById(studentId).orElse(null);

        UUID mentorId = SecurityUtil.getCurrentUserId();
        User mentor = userRepository.findById(mentorId).orElse(null);

        if (student != null && mentor != null) {
            MentoringNote note = new MentoringNote();
            note.setId(UUID.randomUUID());
            note.setMentor(mentor);
            note.setStudent(student);
            note.setCategory((String) body.get("category"));
            note.setContent((String) body.get("body"));
            note.setCreatedAt(Instant.now());

            mentoringNoteRepository.save(note);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/mentor/alerts")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<List<MentorAlertResponse>> getAlerts() {
        return ApiResponse.success(List.of());
    }

    @GetMapping("/mentor/action-items")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<List<MentorActionItemResponse>> getActionItems() {
        return ApiResponse.success(List.of());
    }

    private MentorStudentResponse mapToMentorStudentResponse(User student) {
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(student.getId());
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        String courseTitle = "Unenrolled";
        if (!enrollments.isEmpty()) {
            courseTitle = enrollments.get(0).getCourse().getTitle();
        }

        return MentorStudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .courseTitle(courseTitle)
                .batchName("Batch 2026-A")
                .skills(profileOpt.map(StudentProfile::getSkills).orElse(List.of("Java")))
                .progressPercent(75.0)
                .attendancePercent(92.0)
                .careerReadiness(75)
                .quizScore(84.0)
                .codingScore(78.0)
                .lastSessionAt(null)
                .nextSessionAt(null)
                .status("on_track")
                .build();
    }

    private MentorSessionResponse mapToSessionResponse(MentoringSession session) {
        return MentorSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .kind(session.getKind())
                .mentorId(session.getMentor().getId())
                .studentId(session.getStudent().getId())
                .studentName(session.getStudent().getName())
                .date(session.getSessionDate() != null ? session.getSessionDate().toString() : null)
                .time(session.getSessionTime())
                .durationMinutes(session.getDurationMinutes())
                .platform(session.getPlatform())
                .meetingUrl(session.getMeetingUrl())
                .agenda(session.getAgenda())
                .notes(session.getNotes())
                .status(session.getStatus())
                .build();
    }

    private MentorNoteResponse mapToNoteResponse(MentoringNote note) {
        return MentorNoteResponse.builder()
                .id(note.getId())
                .mentorId(note.getMentor().getId())
                .studentId(note.getStudent().getId())
                .studentName(note.getStudent().getName())
                .category(note.getCategory())
                .body(note.getContent())
                .at(note.getCreatedAt().toString())
                .isPrivate(true)
                .build();
    }
}
