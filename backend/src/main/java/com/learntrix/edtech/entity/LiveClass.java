package com.learntrix.edtech.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.learntrix.edtech.common.audit.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "live_classes")
@Getter
@Setter
public class LiveClass extends AuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "trainer_name", nullable = false)
    private String trainerName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String platform;

    @Column(name = "meeting_url")
    private String meetingUrl;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String visibility;

    @Column(nullable = false)
    private Boolean published = true;

    @Column(nullable = false)
    private String status = "Upcoming";

    @Column(nullable = false)
    private Integer registrations = 0;
}
