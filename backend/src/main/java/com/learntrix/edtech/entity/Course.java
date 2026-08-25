package com.learntrix.edtech.entity;

import com.learntrix.edtech.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
public class Course extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(length = 100)
    private String category;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    /* Catalogue attributes the storefront filters and sorts on (migration V21). */

    @Column(nullable = false, length = 20)
    private String level = "Intermediate";

    @Column(nullable = false, length = 50)
    private String language = "English";

    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours = 0;

    @Column(name = "lesson_count", nullable = false)
    private Integer lessonCount = 0;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(name = "student_count", nullable = false)
    private Integer studentCount = 0;

    @Column(nullable = false)
    private Integer price = 0;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Module> modules = new ArrayList<>();
}
