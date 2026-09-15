package br.com.espacosinapse.clinic;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "clinic_settings")
public class ClinicSettings extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 32)
    private String whatsapp;

    @Column(length = 500)
    private String publicAddress;

    private boolean addressConfirmed;

    protected ClinicSettings() {
    }

    void updatePublicInformation(
        String displayName,
        String description,
        String phone,
        String email,
        String whatsapp,
        String publicAddress,
        boolean addressConfirmed
    ) {
        this.displayName = Objects.requireNonNull(displayName);
        this.description = Objects.requireNonNull(description);
        this.phone = Objects.requireNonNull(phone);
        this.email = Objects.requireNonNull(email);
        this.whatsapp = Objects.requireNonNull(whatsapp);
        this.publicAddress = publicAddress;
        this.addressConfirmed = addressConfirmed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public boolean isAddressConfirmed() {
        return addressConfirmed;
    }
}
