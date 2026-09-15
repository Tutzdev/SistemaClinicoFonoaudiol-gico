package br.com.espacosinapse.clinic;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinic_settings")
public class ClinicSettings extends BaseEntity {
    @Column(nullable = false, length = 160)
    public String displayName;

    @Column(nullable = false, length = 4000)
    public String description;

    @Column(nullable = false, length = 32)
    public String phone;

    @Column(nullable = false, length = 254)
    public String email;

    @Column(nullable = false, length = 32)
    public String whatsapp;

    @Column(length = 500)
    public String publicAddress;

    public boolean addressConfirmed;
}
