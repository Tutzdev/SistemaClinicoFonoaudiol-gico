package br.com.espacosinapse.professionals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "availability_blocks")
public class AvailabilityBlock {
    @Id
    public UUID id = UUID.randomUUID();

    @Column(nullable = false)
    public UUID professionalId;

    @Column(name = "starts_at", nullable = false)
    public Instant start;

    @Column(name = "ends_at", nullable = false)
    public Instant end;
}
