package br.com.espacosinapse.professionals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_periods")
public class AvailabilityPeriod {
    @Id
    public UUID id = UUID.randomUUID();

    @Column(nullable = false)
    public UUID professionalId;

    public int dayOfWeek;

    public LocalTime startTime;

    public LocalTime endTime;
}
