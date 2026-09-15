package br.com.espacosinapse.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    public UUID id = UUID.randomUUID();

    public UUID actorId;

    @Column(nullable = false, length = 40)
    public String action;

    @Column(nullable = false, length = 60)
    public String entityType;

    @Column(nullable = false)
    public UUID entityId;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
