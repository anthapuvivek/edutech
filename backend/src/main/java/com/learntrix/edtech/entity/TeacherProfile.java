package com.learntrix.edtech.entity;

import com.learntrix.edtech.common.audit.AuditableEntity;
import com.learntrix.edtech.common.util.PostgresStringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teacher_profiles")
@Getter
@Setter
public class TeacherProfile extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 20)
    private String phone;

    private String headline;
    private String department;
    private String qualification;

    @Column(name = "experience_years")
    private Integer experienceYears = 0;

    @Convert(converter = PostgresStringListConverter.class)
    private List<String> skills;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "approval_status", nullable = false, length = 50)
    private String approvalStatus = "approved";

    private java.math.BigDecimal rating = java.math.BigDecimal.valueOf(5.0);

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "cv_url", length = 512)
    private String cvUrl;

    @Column(name = "linkedin_url", length = 512)
    private String linkedinUrl;

    @Column(name = "github_url", length = 512)
    private String githubUrl;
}
