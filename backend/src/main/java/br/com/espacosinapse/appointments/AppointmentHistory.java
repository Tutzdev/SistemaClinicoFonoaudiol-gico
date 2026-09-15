package br.com.espacosinapse.appointments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointment_history")
public class AppointmentHistory {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID appointmentId;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant previousStart;

    private Instant newStart;

    @Column(length = 30)
    private String previousStatus;

    @Column(length = 30)
    private String newStatus;

    protected AppointmentHistory() {
    }

    public AppointmentHistory(
        UUID appointmentId,
        UUID actorId,
        String action,
        Instant previousStart,
        Instant newStart,
        String previousStatus,
        String newStatus
    ) {
        this.appointmentId = Objects.requireNonNull(appointmentId);
        this.actorId = Objects.requireNonNull(actorId);
        this.action = Objects.requireNonNull(action);
        this.previousStart = previousStart;
        this.newStart = Objects.requireNonNull(newStart);
        this.previousStatus = previousStatus;
        this.newStatus = Objects.requireNonNull(newStatus);
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPreviousStart() {
        return previousStart;
    }

    public Instant getNewStart() {
        return newStart;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }
}
