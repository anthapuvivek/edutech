package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.student.ActivityItemResponse;
import com.learntrix.edtech.dto.student.StudentProfileResponse;
import com.learntrix.edtech.dto.student.StudentStatsResponse;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RecordingWatchProgressRepository progressRepository;

    public StudentService(
            StudentProfileRepository studentProfileRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            RecordingWatchProgressRepository progressRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(UUID userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "userId", userId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        int threshold = 10000;
        int points = profile.getPoints() != null ? profile.getPoints() : 0;
        int pointsToNext = Math.max(0, threshold - points);

        return StudentProfileResponse.builder()
                .id(profile.getId())
                .studentId(profile.getStudentId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .level(profile.getLevelName())
                .nextLevel("Expert")
                .points(points)
                .pointsToNextLevel(pointsToNext)
                .nextLevelThreshold(threshold)
                .rank(profile.getRankVal() != null ? profile.getRankVal() : 1)
                .streakDays(profile.getStreakDays() != null ? profile.getStreakDays() : 0)
                .build();
    }

    @Transactional(readOnly = true)
    public StudentStatsResponse getStats(UUID userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Fallback stub if profile doesn't exist for test users
                    StudentProfile sp = new StudentProfile();
                    sp.setPoints(100);
                    sp.setRankVal(1);
                    return sp;
                });

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(userId);
        
        int total = enrollments.size();
        int active = 0;
        int completed = 0;
        for (Enrollment e : enrollments) {
            if ("active".equalsIgnoreCase(e.getStatus())) {
                active++;
            } else if ("completed".equalsIgnoreCase(e.getStatus())) {
                completed++;
            }
        }

        // Calculate hours based on completed watch progress
        List<RecordingWatchProgress> progressList = progressRepository.findAll().stream()
                .filter(p -> p.getStudent().getId().equals(userId))
                .toList();

        long totalWatchedSeconds = 0;
        for (RecordingWatchProgress progress : progressList) {
            totalWatchedSeconds += progress.getWatchedSeconds();
        }
        int learningHours = (int) (totalWatchedSeconds / 3600);
        if (learningHours == 0 && !progressList.isEmpty()) {
            learningHours = 1; // Fallback default
        }

        return StudentStatsResponse.builder()
                .totalCourses(total)
                .activeCourses(active)
                .completedCourses(completed)
                .learningHours(learningHours)
                .problemsSolved(24) // Default/stub until Phase 5 coding practice table is live
                .quizAverage(88)    // Default/stub until Phase 4 quiz runner is live
                .points(profile.getPoints() != null ? profile.getPoints() : 0)
                .rank(profile.getRankVal() != null ? profile.getRankVal() : 1)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ActivityItemResponse> getActivity(UUID userId) {
        // Return structured mock learning log until gamification activity logs table is setup in Phase 6
        List<ActivityItemResponse> list = new ArrayList<>();
        list.add(ActivityItemResponse.builder()
                .id(UUID.randomUUID())
                .kind("class")
                .title("Attended Live Session")
                .points(15)
                .occurredAt(Instant.now().minusSeconds(3600 * 2))
                .build());
        list.add(ActivityItemResponse.builder()
                .id(UUID.randomUUID())
                .kind("lesson")
                .title("Watched recorded class on Spring Boot JPA")
                .points(10)
                .occurredAt(Instant.now().minusSeconds(3600 * 24))
                .build());
        list.add(ActivityItemResponse.builder()
                .id(UUID.randomUUID())
                .kind("coding")
                .title("Solved Array Challenges")
                .points(20)
                .occurredAt(Instant.now().minusSeconds(3600 * 48))
                .build());
        return list;
    }

    @Transactional(readOnly = true)
    public List<com.learntrix.edtech.dto.student.LeaderboardEntryResponse> getLeaderboard(UUID currentUserId, String scope) {
        List<StudentProfile> profiles = studentProfileRepository.findAll();
        List<com.learntrix.edtech.dto.student.LeaderboardEntryResponse> entries = new ArrayList<>();
        int rank = 1;
        for (StudentProfile profile : profiles) {
            User user = userRepository.findById(profile.getUserId()).orElse(null);
            if (user != null) {
                entries.add(com.learntrix.edtech.dto.student.LeaderboardEntryResponse.builder()
                        .rank(rank++)
                        .studentId(profile.getStudentId())
                        .name(user.getName())
                        .points(profile.getPoints() != null ? profile.getPoints() : 0)
                        .problemsSolved(24)
                        .quizScore(85)
                        .attendance(92)
                        .streak(profile.getStreakDays() != null ? profile.getStreakDays() : 0)
                        .level(profile.getLevelName() != null ? profile.getLevelName() : "Beginner")
                        .isCurrentUser(currentUserId != null && currentUserId.equals(profile.getUserId()))
                        .build());
            }
        }
        return entries;
    }
}
