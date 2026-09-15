package br.com.espacosinapse.patients;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient extends BaseEntity {
    @Column(nullable = false, length = 160)
    public String name;

    @Column(nullable = false)
    public LocalDate birthDate;

    @Column(nullable = false, length = 32)
    public String phone;

    @Column(length = 254)
    public String email;

    @Column(length = 160)
    public String guardianName;

    @Column(length = 80)
    public String guardianRelationship;

    @Column(length = 32)
    public String guardianPhone;

    public boolean active = true;
}
