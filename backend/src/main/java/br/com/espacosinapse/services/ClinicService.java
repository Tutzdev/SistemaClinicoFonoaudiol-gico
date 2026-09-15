package br.com.espacosinapse.services;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "clinic_services")
public class ClinicService extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 4000)
    private String description;

    private int durationMinutes;

    private boolean active = true;

    private boolean published;

    protected ClinicService() {
    }

    ClinicService(String name, String description, int durationMinutes, boolean published) {
        updateDetails(name, description, durationMinutes, published);
    }

    public ClinicService(UUID id, String name, String description, int durationMinutes) {
        super(id);
        updateDetails(name, description, durationMinutes, false);
    }

    void updateDetails(String name, String description, int durationMinutes, boolean published) {
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.published = published;
    }

    void changeActiveStatus(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPublished() {
        return published;
    }
}
