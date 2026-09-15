package br.com.espacosinapse.services;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinic_services")
public class ClinicService extends BaseEntity {
    @Column(nullable = false, length = 160)
    public String name;

    @Column(length = 4000)
    public String description;

    public int durationMinutes;

    public boolean active = true;

    public boolean published;
}
