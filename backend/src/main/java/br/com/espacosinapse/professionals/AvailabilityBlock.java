package br.com.espacosinapse.professionals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "availability_blocks")
public class AvailabilityBlock {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID professionalId;

    @Column(name = "starts_at", nullable = false)
    private Instant start;

    @Column(name = "ends_at", nullable = false)
    private Instant end;

    protected AvailabilityBlock() {
    }

    public AvailabilityBlock(UUID professionalId, Instant start, Instant end) {
        this.professionalId = Objects.requireNonNull(professionalId);
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfessionalId() {
        return professionalId;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }
}
