package br.com.espacosinapse.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    public UUID id = UUID.randomUUID();

    @Version
    public long version;

    @Column(nullable = false, updatable = false)
    public Instant createdAt = Instant.now();

    @Column(nullable = false)
    public Instant updatedAt = Instant.now();

    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
