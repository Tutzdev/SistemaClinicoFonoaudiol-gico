package br.com.espacosinapse.appointments;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
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
    public UUID patientId;

    @Column(nullable = false)
    public UUID professionalId;

    @Column(nullable = false)
    public UUID serviceId;

    @Column(nullable = false)
    public UUID createdBy;

    @Column(name = "starts_at", nullable = false)
    public Instant start;

    @Column(name = "ends_at", nullable = false)
    public Instant end;

    public int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public Status status = Status.AGENDADO;

    public boolean isOpen() {
        return status == Status.AGENDADO || status == Status.CONFIRMADO;
    }
}
