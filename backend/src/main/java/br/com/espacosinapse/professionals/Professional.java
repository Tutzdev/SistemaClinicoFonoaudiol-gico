package br.com.espacosinapse.professionals;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "professionals")
public class Professional extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(length = 80)
    private String registration;

    @Column(length = 80)
    private String region;

    @Column(length = 4000)
    private String bio;

    private boolean active = true;

    private boolean published;

    @ElementCollection
    @CollectionTable(name = "professional_services", joinColumns = @JoinColumn(name = "professional_id"))
    @Column(name = "service_id")
    private Set<UUID> serviceIds = new HashSet<>();

    protected Professional() {
    }

    Professional(
        String name,
        String email,
        String phone,
        String registration,
        String region,
        String bio,
        Set<UUID> serviceIds,
        boolean published
    ) {
        updateDetails(name, email, phone, registration, region, bio, serviceIds, published);
    }

    public Professional(UUID id, String name, Set<UUID> serviceIds) {
        super(id);
        updateDetails(name, null, null, null, null, null, serviceIds, false);
    }

    void updateDetails(
        String name,
        String email,
        String phone,
        String registration,
        String region,
        String bio,
        Set<UUID> serviceIds,
        boolean published
    ) {
        this.name = Objects.requireNonNull(name);
        this.email = email;
        this.phone = phone;
        this.registration = registration;
        this.region = region;
        this.bio = bio;
        this.published = published;
        this.serviceIds.clear();
        this.serviceIds.addAll(Set.copyOf(serviceIds));
    }

    void changeActiveStatus(boolean active) {
        this.active = active;
    }

    public boolean offersService(UUID serviceId) {
        return serviceIds.contains(serviceId);
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRegistration() {
        return registration;
    }

    public String getRegion() {
        return region;
    }

    public String getBio() {
        return bio;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPublished() {
        return published;
    }

    public Set<UUID> getServiceIds() {
        return Set.copyOf(serviceIds);
    }
}
