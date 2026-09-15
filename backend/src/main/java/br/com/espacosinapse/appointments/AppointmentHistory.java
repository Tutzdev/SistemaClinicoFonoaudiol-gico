package br.com.espacosinapse.appointments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment_history")
public class AppointmentHistory {
    @Id
    public UUID id = UUID.randomUUID();

    @Column(nullable = false)
    public UUID appointmentId;

    @Column(nullable = false)
    public UUID actorId;

    @Column(nullable = false, length = 40)
    public String action;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant previousStart;

    public Instant newStart;

    @Column(length = 30)
    public String previousStatus;

    @Column(length = 30)
    public String newStatus;
}
