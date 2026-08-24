package com.learntrix.edtech.entity;

import com.learntrix.edtech.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

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
