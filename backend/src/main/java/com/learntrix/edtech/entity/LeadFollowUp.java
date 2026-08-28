package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lead_follow_ups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadFollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "follow_up_date", nullable = false, length = 20)
    private String followUpDate;

    @Column(name = "follow_up_time", nullable = false, length = 20)
    private String followUpTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "next_action", nullable = false, length = 100)
    @Builder.Default
    private String nextAction = "Call";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "Pending";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
