package br.com.espacosinapse.appointments;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {
    public enum Status {
        AGENDADO,
        CONFIRMADO,
        CONCLUIDO,
        CANCELADO,
        NAO_COMPARECEU
    }

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID professionalId;

    @Column(nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(name = "starts_at", nullable = false)
    private Instant start;

    @Column(name = "ends_at", nullable = false)
    private Instant end;

    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.AGENDADO;

    protected Appointment() {
    }

    Appointment(
        UUID patientId,
        UUID professionalId,
        UUID serviceId,
        UUID createdBy,
        Instant start,
        int durationMinutes
    ) {
        this.patientId = Objects.requireNonNull(patientId);
        this.professionalId = Objects.requireNonNull(professionalId);
        this.serviceId = Objects.requireNonNull(serviceId);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.durationMinutes = durationMinutes;
        scheduleAt(start);
    }

    void reschedule(Instant start) {
        scheduleAt(start);
        status = Status.AGENDADO;
    }

    private void scheduleAt(Instant start) {
        this.start = Objects.requireNonNull(start);
        end = start.plusSeconds(durationMinutes * 60L);
    }

    void changeStatus(Status status) {
        this.status = Objects.requireNonNull(status);
    }

    public boolean isOpen() {
        return status == Status.AGENDADO || status == Status.CONFIRMADO;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getProfessionalId() {
        return professionalId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Status getStatus() {
        return status;
    }
}
