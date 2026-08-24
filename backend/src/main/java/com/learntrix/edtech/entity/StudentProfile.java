package com.learntrix.edtech.entity;

import com.learntrix.edtech.common.audit.AuditableEntity;
import com.learntrix.edtech.common.util.PostgresStringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
public class StudentProfile extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "student_id", nullable = false, unique = true, length = 20)
    private String studentId;

    @Column(name = "full_name")
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    private String education;
    private String college;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    private String qualification;
    private String location;

    @Column(length = 100)
    private String city;

    @Convert(converter = PostgresStringListConverter.class)
    private List<String> skills;

    @Column(name = "github_url", length = 512)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 512)
    private String linkedinUrl;

    @Column(name = "leetcode_url", length = 512)
    private String leetcodeUrl;

    @Column(name = "hackerrank_url", length = 512)
    private String hackerrankUrl;

    @Column(name = "portfolio_url", length = 512)
    private String portfolioUrl;

    @Column(name = "career_goal", length = 500)
    private String careerGoal;

    @Column(name = "preferred_role")
    private String preferredRole;

    @Convert(converter = PostgresStringListConverter.class)
    @Column(name = "preferred_locations")
    private List<String> preferredLocations;

    @Column(name = "experience_level", length = 50)
    private String experienceLevel;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_completion_percent", nullable = false)
    private Integer profileCompletionPercent = 0;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(name = "rank_val", nullable = false)
    private Integer rankVal = 0;

    @Column(name = "streak_days", nullable = false)
    private Integer streakDays = 0;

    @Column(name = "level_name", nullable = false, length = 50)
    private String levelName = "Beginner";
}
