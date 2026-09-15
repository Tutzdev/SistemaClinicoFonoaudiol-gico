package br.com.espacosinapse.patients;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 160)
    private String guardianName;

    @Column(length = 80)
    private String guardianRelationship;

    @Column(length = 32)
    private String guardianPhone;

    private boolean active = true;

    protected Patient() {
    }

    Patient(
        String name,
        LocalDate birthDate,
        String phone,
        String email,
        String guardianName,
        String guardianRelationship,
        String guardianPhone
    ) {
        updateDetails(name, birthDate, phone, email, guardianName, guardianRelationship, guardianPhone);
    }

    public Patient(UUID id, String name, LocalDate birthDate, String phone) {
        super(id);
        updateDetails(name, birthDate, phone, null, null, null, null);
    }

    void updateDetails(
        String name,
        LocalDate birthDate,
        String phone,
        String email,
        String guardianName,
        String guardianRelationship,
        String guardianPhone
    ) {
        this.name = Objects.requireNonNull(name);
        this.birthDate = Objects.requireNonNull(birthDate);
        this.phone = Objects.requireNonNull(phone);
        this.email = email;
        this.guardianName = guardianName;
        this.guardianRelationship = guardianRelationship;
        this.guardianPhone = guardianPhone;
    }

    void changeActiveStatus(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public String getGuardianRelationship() {
        return guardianRelationship;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public boolean isActive() {
        return active;
    }
}
