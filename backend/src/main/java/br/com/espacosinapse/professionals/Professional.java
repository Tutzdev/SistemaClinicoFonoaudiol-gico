package br.com.espacosinapse.professionals;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "professionals")
public class Professional extends BaseEntity {
    @Column(nullable = false, length = 160)
    public String name;

    @Column(length = 254)
    public String email;

    @Column(length = 32)
    public String phone;

    @Column(length = 80)
    public String registration;

    @Column(length = 80)
    public String region;

    @Column(length = 4000)
    public String bio;

    public boolean active = true;

    public boolean published;

    @ElementCollection
    @CollectionTable(name = "professional_services", joinColumns = @JoinColumn(name = "professional_id"))
    @Column(name = "service_id")
    public Set<UUID> serviceIds = new HashSet<>();
}
