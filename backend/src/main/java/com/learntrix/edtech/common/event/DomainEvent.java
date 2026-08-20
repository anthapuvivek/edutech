package com.learntrix.edtech.common.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final Instant occurredAt = Instant.now();
    private final String eventType;

    protected DomainEvent(String eventType) {
        this.eventType = eventType;
    }
}
