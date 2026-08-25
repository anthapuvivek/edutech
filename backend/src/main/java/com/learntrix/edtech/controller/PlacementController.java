package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.placement.*;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/placement")
@PreAuthorize("hasAnyRole('PLACEMENT_OFFICER', 'ADMIN', 'SUPER_ADMIN')")
public class PlacementController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementInterviewRepository placementInterviewRepository;
    private final PlacementOfferRepository placementOfferRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;

    public PlacementController(
            UserRepository userRepository,
            JobRepository jobRepository,
            JobApplicationRepository jobApplicationRepository,
            PlacementDriveRepository placementDriveRepository,
            PlacementInterviewRepository placementInterviewRepository,
            PlacementOfferRepository placementOfferRepository,
            EnrollmentRepository enrollmentRepository,
            StudentProfileRepository studentProfileRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementInterviewRepository = placementInterviewRepository;
        this.placementOfferRepository = placementOfferRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    // Read-only transaction: the response mappers walk LAZY @ManyToOne relations
    // (Enrollment.course, Job.company, ...) and open-in-view is disabled, so without
    // an open session these endpoints fail with LazyInitializationException.
    @Transactional(readOnly = true)
    @GetMapping("/stats")
    public ApiResponse<PlacementStatsResponse> getStats() {
        int studentCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .count();

        int driveCount = (int) placementDriveRepository.count();
        int activeJobs = (int) jobRepository.count();
        int appCount = (int) jobApplicationRepository.count();
        int interviewCount = (int) placementInterviewRepository.count();
        int offerCount = (int) placementOfferRepository.count();

        return ApiResponse.success(PlacementStatsResponse.builder()
                .eligibleStudents(studentCount)
                .activeJobs(activeJobs)
                .placementDrives(driveCount)
                .applications(appCount)
                .shortlisted(Math.max(0, appCount - 1))
                .interviews(interviewCount)
                .offers(offerCount)
                .placements(offerCount)
                .build());
    }

    @Transactional(readOnly = true)
    @GetMapping("/analytics")
    public ApiResponse<PlacementAnalyticsResponse> getAnalytics() {
        int studentCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .count();

        List<PlacementAnalyticsResponse.FunnelPoint> funnel = List.of(
                new PlacementAnalyticsResponse.FunnelPoint("Registered", studentCount),
                new PlacementAnalyticsResponse.FunnelPoint("Eligible", studentCount),
                new PlacementAnalyticsResponse.FunnelPoint("Applied", (int) jobApplicationRepository.count()),
                new PlacementAnalyticsResponse.FunnelPoint("Placed", (int) placementOfferRepository.count())
        );

        return ApiResponse.success(PlacementAnalyticsResponse.builder()
                .totalEligible(studentCount)
                .totalApplied((int) jobApplicationRepository.count())
                .shortlisted((int) jobApplicationRepository.count())
                .interviewed((int) placementInterviewRepository.count())
                .offers((int) placementOfferRepository.count())
                .placed((int) placementOfferRepository.count())
                .funnel(funnel)
                .drivesProgress(List.of())
                .build());
    }

    @Transactional(readOnly = true)
    @GetMapping("/students")
    public ApiResponse<List<PlacementEligibleStudentResponse>> getStudents(
            @RequestParam(value = "eligibility", required = false) String eligibility,
            @RequestParam(value = "search", required = false) String search) {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        List<PlacementEligibleStudentResponse> responses = students.stream()
                .map(this::mapToEligibleStudentResponse)
                .filter(s -> {
                    if (eligibility != null && !"all".equalsIgnoreCase(eligibility)) {
                        return s.getEligibility().equalsIgnoreCase(eligibility);
                    }
                    return true;
                })
                .filter(s -> {
                    if (search != null && !search.trim().isEmpty()) {
                        return s.getName().toLowerCase().contains(search.toLowerCase())
                                || s.getCourseTitle().toLowerCase().contains(search.toLowerCase());
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @Transactional(readOnly = true)
    @GetMapping("/drives")
    public ApiResponse<List<PlacementDriveResponse>> getDrives() {
        List<PlacementDrive> drives = placementDriveRepository.findAll();
        List<PlacementDriveResponse> responses = drives.stream()
                .map(this::mapToDriveResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping("/drives")
    public ApiResponse<Map<String, Object>> createDrive(@RequestBody Map<String, Object> body) {
        PlacementDrive drive = new PlacementDrive();
        drive.setId(UUID.randomUUID());
        drive.setCompanyName((String) body.get("companyName"));
        drive.setRole((String) body.get("role"));
        drive.setDescription((String) body.get("description"));
        drive.setEligibilitySummary((String) body.get("eligibilitySummary"));
        drive.setLocation((String) body.get("location"));
        drive.setCtcRange((String) body.get("ctcRange"));
        drive.setOpenings(body.containsKey("openings") ? (Integer) body.get("openings") : 1);
        drive.setStage("Draft");
        
        if (body.containsKey("applicationDeadline")) {
            drive.setApplicationDeadline(Instant.parse((String) body.get("applicationDeadline")));
        }

        placementDriveRepository.save(drive);
        return ApiResponse.success(Map.of("ok", true));
    }

    @PatchMapping("/drives/{id}/stage")
    public ApiResponse<Map<String, Object>> updateDriveStage(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {
        PlacementDrive drive = placementDriveRepository.findById(id).orElse(null);
        if (drive != null) {
            drive.setStage(body.get("stage"));
            placementDriveRepository.save(drive);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    @Transactional(readOnly = true)
    @GetMapping("/applications")
    public ApiResponse<List<PlacementApplicationResponse>> getApplications(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search) {
        List<JobApplication> apps = jobApplicationRepository.findAll();
        List<PlacementApplicationResponse> responses = apps.stream()
                .map(this::mapToApplicationResponse)
                .filter(a -> {
                    if (status != null && !"all".equalsIgnoreCase(status)) {
                        return a.getStatus().equalsIgnoreCase(status);
                    }
                    return true;
                })
                .filter(a -> {
                    if (search != null && !search.trim().isEmpty()) {
                        return a.getStudentName().toLowerCase().contains(search.toLowerCase())
                                || a.getCompanyName().toLowerCase().contains(search.toLowerCase());
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    @PatchMapping("/applications/status")
    public ApiResponse<Map<String, Object>> updateApplicationStatus(@RequestBody Map<String, Object> body) {
        List<String> idsStr = (List<String>) body.get("ids");
        String status = (String) body.get("status");

        for (String idStr : idsStr) {
            UUID id = UUID.fromString(idStr);
            JobApplication app = jobApplicationRepository.findById(id).orElse(null);
            if (app != null) {
                app.setStatus(status);
                jobApplicationRepository.save(app);
            }
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    @Transactional(readOnly = true)
    @GetMapping("/interviews")
    public ApiResponse<List<PlacementInterviewResponse>> getInterviews() {
        List<PlacementInterview> interviews = placementInterviewRepository.findAll();
        List<PlacementInterviewResponse> responses = interviews.stream()
                .map(this::mapToInterviewResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping("/interviews")
    public ApiResponse<Map<String, Object>> scheduleInterview(@RequestBody Map<String, Object> body) {
        UUID studentId = UUID.fromString((String) body.get("studentId"));
        User student = userRepository.findById(studentId).orElse(null);

        if (student != null) {
            PlacementInterview interview = new PlacementInterview();
            interview.setId(UUID.randomUUID());
            interview.setStudent(student);
            interview.setCompanyName((String) body.get("companyName"));
            interview.setRole((String) body.get("role"));
            interview.setRound((String) body.get("round"));
            interview.setInterviewTime((String) body.get("time"));
            interview.setInterviewer((String) body.get("interviewer"));
            interview.setPlatform((String) body.get("platform"));
            interview.setMeetingUrl((String) body.get("meetingUrl"));
            interview.setStatus("Scheduled");

            if (body.containsKey("date")) {
                interview.setInterviewDate(LocalDate.parse((String) body.get("date")));
            }

            placementInterviewRepository.save(interview);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    @Transactional(readOnly = true)
    @GetMapping("/offers")
    public ApiResponse<List<PlacementOfferResponse>> getOffers() {
        List<PlacementOffer> offers = placementOfferRepository.findAll();
        List<PlacementOfferResponse> responses = offers.stream()
                .map(this::mapToOfferResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping("/offers")
    public ApiResponse<Map<String, Object>> recordOffer(@RequestBody Map<String, Object> body) {
        UUID studentId = UUID.fromString((String) body.get("studentId"));
        User student = userRepository.findById(studentId).orElse(null);

        if (student != null) {
            PlacementOffer offer = new PlacementOffer();
            offer.setId(UUID.randomUUID());
            offer.setStudent(student);
            offer.setCompanyName((String) body.get("companyName"));
            offer.setRole((String) body.get("role"));
            offer.setLocation((String) body.get("location"));
            offer.setStatus("Offer Received");

            if (body.containsKey("offerDate")) {
                offer.setOfferDate(LocalDate.parse((String) body.get("offerDate")));
            }
            if (body.containsKey("joiningDate")) {
                offer.setJoiningDate(LocalDate.parse((String) body.get("joiningDate")));
            }
            if (body.containsKey("annualSalary")) {
                Object val = body.get("annualSalary");
                if (val instanceof Number) {
                    offer.setAnnualSalary(((Number) val).doubleValue());
                } else if (val instanceof String) {
                    offer.setAnnualSalary(Double.parseDouble((String) val));
                }
            }

            placementOfferRepository.save(offer);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    @PatchMapping("/offers/{id}/status")
    public ApiResponse<Map<String, Object>> updateOfferStatus(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {
        PlacementOffer offer = placementOfferRepository.findById(id).orElse(null);
        if (offer != null) {
            offer.setStatus(body.get("status"));
            placementOfferRepository.save(offer);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    private PlacementEligibleStudentResponse mapToEligibleStudentResponse(User student) {
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(student.getId());
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        String courseTitle = "Unenrolled";
        if (!enrollments.isEmpty()) {
            courseTitle = enrollments.get(0).getCourse().getTitle();
        }

        return PlacementEligibleStudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .courseTitle(courseTitle)
                .batchName("Batch 2026-A")
                .skills(profileOpt.map(StudentProfile::getSkills).orElse(List.of("Java")))
                .attendancePercent(92.0)
                .progressPercent(80.0)
                .codingScore(78.0)
                .resumeStatus("ATS Verified")
                .careerReadiness(75)
                .eligibility("eligible")
                .placementStatus("Applied")
                .build();
    }

    private PlacementDriveResponse mapToDriveResponse(PlacementDrive drive) {
        return PlacementDriveResponse.builder()
                .id(drive.getId())
                .companyName(drive.getCompanyName())
                .role(drive.getRole())
                .description(drive.getDescription())
                .eligibilitySummary(drive.getEligibilitySummary())
                .applicationDeadline(drive.getApplicationDeadline() != null ? drive.getApplicationDeadline().toString() : null)
                .assessmentDate(drive.getAssessmentDate() != null ? drive.getAssessmentDate().toString() : null)
                .interviewDate(drive.getInterviewDate() != null ? drive.getInterviewDate().toString() : null)
                .openings(drive.getOpenings())
                .eligibleStudents(4)
                .registeredStudents(2)
                .stage(drive.getStage())
                .location(drive.getLocation())
                .ctcRange(drive.getCtcRange())
                .build();
    }

    private PlacementApplicationResponse mapToApplicationResponse(JobApplication app) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(app.getStudent().getId());
        String courseTitle = enrollments.isEmpty() ? "General Track" : enrollments.get(0).getCourse().getTitle();

        return PlacementApplicationResponse.builder()
                .id(app.getId())
                .studentId(app.getStudent().getId())
                .studentName(app.getStudent().getName())
                .companyName(app.getJob().getCompany().getName())
                .role(app.getJob().getTitle())
                .courseTitle(courseTitle)
                .eligibility("eligible")
                .appliedAt(app.getAppliedAt().toString())
                .status(app.getStatus())
                .interviewAt(null)
                .result(null)
                .driveId(null)
                .build();
    }

    private PlacementInterviewResponse mapToInterviewResponse(PlacementInterview interview) {
        return PlacementInterviewResponse.builder()
                .id(interview.getId())
                .studentName(interview.getStudent().getName())
                .studentId(interview.getStudent().getId())
                .companyName(interview.getCompanyName())
                .role(interview.getRole())
                .round(interview.getRound())
                .date(interview.getInterviewDate() != null ? interview.getInterviewDate().toString() : null)
                .time(interview.getInterviewTime())
                .interviewer(interview.getInterviewer())
                .platform(interview.getPlatform())
                .meetingUrl(interview.getMeetingUrl())
                .status(interview.getStatus())
                .build();
    }

    private PlacementOfferResponse mapToOfferResponse(PlacementOffer offer) {
        return PlacementOfferResponse.builder()
                .id(offer.getId())
                .studentId(offer.getStudent().getId())
                .studentName(offer.getStudent().getName())
                .companyName(offer.getCompanyName())
                .role(offer.getRole())
                .offerDate(offer.getOfferDate() != null ? offer.getOfferDate().toString() : null)
                .joiningDate(offer.getJoiningDate() != null ? offer.getJoiningDate().toString() : null)
                .annualSalary(offer.getAnnualSalary())
                .location(offer.getLocation())
                .status(offer.getStatus())
                .verifiedBy("Admin")
                .build();
    }
}
