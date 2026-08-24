package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.career.*;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/career")
@PreAuthorize("hasRole('STUDENT')")
public class JobController {

    private final StudentProfileRepository studentProfileRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    public JobController(
            StudentProfileRepository studentProfileRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            CompanyRepository companyRepository,
            JobRepository jobRepository,
            JobApplicationRepository jobApplicationRepository,
            UserRepository userRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/eligibility")
    public ApiResponse<CareerEligibilityResponse> getEligibility() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(studentId);
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        boolean onboarded = profileOpt.map(p -> p.getPreferredRole() != null).orElse(false);

        List<CareerEligibilityResponse.Program> programs = enrollments.stream()
                .map(e -> CareerEligibilityResponse.Program.builder()
                        .courseSlug(e.getCourse().getSlug())
                        .courseTitle(e.getCourse().getTitle())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(CareerEligibilityResponse.builder()
                .state("eligible")
                .reason("")
                .eligiblePrograms(programs)
                .onboardingCompleted(onboarded)
                .checkedAt(Instant.now().toString())
                .build());
    }

    @GetMapping("/dashboard")
    public ApiResponse<CareerDashboardResponse> getDashboard() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(studentId);

        // Fetch jobs and applications
        List<Job> jobs = jobRepository.findAll();
        List<JobApplication> applications = jobApplicationRepository.findByStudentId(studentId);

        List<JobResponse> recommendedJobs = jobs.stream()
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());

        List<JobApplicationResponse> applicationResponses = applications.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());

        CareerReadinessResponse readiness = CareerReadinessResponse.builder()
                .overall(75)
                .components(List.of(
                        new CareerReadinessResponse.ComponentScore("Resume", 80),
                        new CareerReadinessResponse.ComponentScore("Quiz Score", 85),
                        new CareerReadinessResponse.ComponentScore("Code practice", 60)
                ))
                .updatedAt(Instant.now().toString())
                .build();

        List<CareerDashboardResponse.ChecklistItem> checklist = List.of(
                new CareerDashboardResponse.ChecklistItem("Resume review", "complete", "Resume is verified by placement officer"),
                new CareerDashboardResponse.ChecklistItem("Profile Onboarding", "complete", "Onboarding profile setup complete"),
                new CareerDashboardResponse.ChecklistItem("Coding challenge", "pending", "Solve 10 more problems to reach optimal preparation level")
        );

        return ApiResponse.success(CareerDashboardResponse.builder()
                .readiness(readiness)
                .placementStatus("Job Seeking")
                .recommendedJobs(recommendedJobs)
                .recommendedInternships(List.of())
                .applications(applicationResponses)
                .upcomingInterviews(List.of())
                .referralOpportunities(List.of())
                .savedJobs(List.of())
                .skillsToImprove(List.of("System Design", "Kafka"))
                .checklist(checklist)
                .notifications(List.of())
                .build());
    }

    @GetMapping("/readiness")
    public ApiResponse<CareerReadinessResponse> getReadiness() {
        return ApiResponse.success(CareerReadinessResponse.builder()
                .overall(75)
                .components(List.of(
                        new CareerReadinessResponse.ComponentScore("Resume", 80),
                        new CareerReadinessResponse.ComponentScore("Quiz Score", 85)
                ))
                .updatedAt(Instant.now().toString())
                .build());
    }

    @GetMapping("/profile")
    public ApiResponse<CareerProfileResponse> getProfile() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        StudentProfile profile = studentProfileRepository.findByUserId(studentId).orElse(null);
        User user = userRepository.findById(studentId).orElse(null);

        if (profile == null || user == null) {
            return ApiResponse.success(null);
        }

        CareerProfileResponse.Links links = CareerProfileResponse.Links.builder()
                .github(profile.getGithubUrl())
                .linkedin(profile.getLinkedinUrl())
                .leetcode(profile.getLeetcodeUrl())
                .hackerrank(profile.getHackerrankUrl())
                .portfolio(profile.getPortfolioUrl())
                .build();

        return ApiResponse.success(CareerProfileResponse.builder()
                .studentId(profile.getStudentId())
                .name(user.getName())
                .headline("Aspiring Backend Software Engineer | Spring Boot Practitioner")
                .education(profile.getEducation() != null ? profile.getEducation() : "B.Tech")
                .courseTitle("Java Backend Developer")
                .skills(profile.getSkills() != null ? profile.getSkills() : List.of())
                .preferredRoles(profile.getPreferredRole() != null ? List.of(profile.getPreferredRole()) : List.of("Software Engineer"))
                .preferredLocations(profile.getPreferredLocations() != null ? profile.getPreferredLocations() : List.of("Hyderabad"))
                .workMode(profile.getPreferredRole() != null ? "Hybrid" : "Remote")
                .experienceLevel(profile.getExperienceLevel() != null ? profile.getExperienceLevel() : "Fresher")
                .availability("Immediate")
                .links(links)
                .completionPercent(profile.getProfileCompletionPercent() != null ? profile.getProfileCompletionPercent() : 75)
                .missingItems(List.of())
                .build());
    }

    @PatchMapping("/profile")
    public ApiResponse<CareerProfileResponse> updateProfile(@RequestBody Map<String, Object> body) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        StudentProfile profile = studentProfileRepository.findByUserId(studentId).orElse(null);
        if (profile != null) {
            if (body.containsKey("headline")) {
                profile.setBio((String) body.get("headline"));
            }
            if (body.containsKey("experienceLevel")) {
                profile.setExperienceLevel((String) body.get("experienceLevel"));
            }
            studentProfileRepository.save(profile);
        }
        return getProfile();
    }

    @PostMapping("/onboarding")
    public ApiResponse<CareerEligibilityResponse> completeOnboarding(@RequestBody CareerOnboardingPayload payload) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        StudentProfile profile = studentProfileRepository.findByUserId(studentId).orElse(null);
        if (profile != null) {
            profile.setPreferredRole(payload.getTargetRole());
            profile.setCareerGoal(payload.getCareerGoal());
            profile.setExperienceLevel(payload.getExperienceLevel());
            if (payload.getPreferredLocation() != null) {
                profile.setPreferredLocations(List.of(payload.getPreferredLocation()));
            }
            studentProfileRepository.save(profile);
        }
        return getEligibility();
    }

    @GetMapping("/roadmap")
    public ApiResponse<CareerRoadmapResponse> getRoadmap() {
        List<CareerRoadmapResponse.RoadmapStep> steps = List.of(
                new CareerRoadmapResponse.RoadmapStep("Core Java Programming", 100),
                new CareerRoadmapResponse.RoadmapStep("Spring Boot Framework & REST API", 75),
                new CareerRoadmapResponse.RoadmapStep("PostgreSQL & Persistence", 50),
                new CareerRoadmapResponse.RoadmapStep("Resume & Mock Interview Prep", 0)
        );

        return ApiResponse.success(CareerRoadmapResponse.builder()
                .goal("Associate Java Backend Engineer")
                .steps(steps)
                .nextRecommended(List.of("Docker Basics", "System Design Patterns"))
                .build());
    }

    @GetMapping("/skill-gap")
    public ApiResponse<SkillGapResponse> getSkillGap() {
        List<SkillGapResponse.NeedsImprovementSkill> gaps = List.of(
                new SkillGapResponse.NeedsImprovementSkill("Spring Boot Security", 2, "java-backend-spring-boot")
        );

        return ApiResponse.success(SkillGapResponse.builder()
                .targetRole("Backend Engineer")
                .strong(List.of("Core Java", "Git"))
                .needsImprovement(gaps)
                .missing(List.of("Kafka", "Kubernetes"))
                .build());
    }

    @GetMapping("/jobs")
    public ApiResponse<List<JobResponse>> getJobs() {
        List<Job> jobs = jobRepository.findAll();
        List<JobResponse> responses = jobs.stream()
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/companies")
    public ApiResponse<List<CompanyResponse>> getCompanies() {
        List<Company> companies = companyRepository.findAll();
        List<CompanyResponse> responses = companies.stream()
                .map(this::mapToCompanyResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/applications")
    public ApiResponse<List<JobApplicationResponse>> getApplications() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<JobApplication> applications = jobApplicationRepository.findByStudentId(studentId);
        List<JobApplicationResponse> responses = applications.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/referrals")
    public ApiResponse<List<ReferralOpportunityResponse>> getReferrals() {
        return ApiResponse.success(List.of());
    }

    private JobResponse mapToJobResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .category("Software Engineering")
                .type(job.getType())
                .location(job.getLocation())
                .workMode(job.getWorkMode())
                .experience("0-1 Years")
                .salaryRange(job.getCtcRange())
                .skills(List.of("Java", "Spring Boot", "SQL"))
                .postedAt(job.getPostedAt().toString())
                .deadline(job.getApplicationDeadline().toString())
                .matchPercent(85)
                .saved(false)
                .build();
    }

    private CompanyResponse mapToCompanyResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .logoUrl(company.getLogoUrl())
                .industry(company.getIndustry())
                .locations(List.of("Bangalore", "Hyderabad"))
                .officialPartner(true)
                .preparationTrack(true)
                .typicalRoles(List.of("Backend Developer", "DevOps Engineer"))
                .requiredSkills(List.of("Java", "Spring Boot"))
                .difficulty("Challenging")
                .averagePreparationWeeks(8)
                .build();
    }

    private JobApplicationResponse mapToApplicationResponse(JobApplication app) {
        return JobApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .companyName(app.getJob().getCompany().getName())
                .role(app.getJob().getTitle())
                .appliedAt(app.getAppliedAt().toString())
                .status(app.getStatus())
                .nextAction(app.getStatus().equalsIgnoreCase("Applied") ? "Awaiting Assessment Scheduling" : "None")
                .deadline(null)
                .interviewAt(null)
                .build();
    }
}
