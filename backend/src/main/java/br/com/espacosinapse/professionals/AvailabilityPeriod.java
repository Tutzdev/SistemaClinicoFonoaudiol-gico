package br.com.espacosinapse.professionals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "availability_periods")
public class AvailabilityPeriod {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID professionalId;

    private int dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    protected AvailabilityPeriod() {
    }

    public AvailabilityPeriod(UUID professionalId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.professionalId = Objects.requireNonNull(professionalId);
        this.dayOfWeek = dayOfWeek;
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
