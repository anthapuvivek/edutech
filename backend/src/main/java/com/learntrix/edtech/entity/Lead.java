package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(name = "course_interest", nullable = false)
    private String courseInterest;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String source = "Website";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String stage = "New";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "last_contact_at", nullable = false)
    @Builder.Default
    private Instant lastContactAt = Instant.now();

    @Column(name = "next_follow_up_at")
    private Instant nextFollowUpAt;

    @Column(length = 100)
    private String city;

    private Integer budget;

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 50;

    @Column(name = "demo_attended", nullable = false)
    @Builder.Default
    private Boolean demoAttended = false;

    @Column(name = "enrollment_status", nullable = false, length = 50)
    @Builder.Default
    private String enrollmentStatus = "Not Enrolled";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LeadNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LeadCommunication> communications = new ArrayList<>();

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LeadFollowUp> followUps = new ArrayList<>();

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Enquiry> enquiries = new ArrayList<>();
}
