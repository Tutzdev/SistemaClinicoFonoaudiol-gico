package br.com.espacosinapse.users;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {
    public enum Role {
        ADMIN,
        RECEPCAO
    }

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private boolean active = true;

    private long authVersion;

    protected AppUser() {
    }

    public AppUser(String name, String email, String passwordHash, Role role) {
        this.name = Objects.requireNonNull(name);
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
    }

    void updateProfile(String name, String email, Role role) {
        String normalizedEmail = normalizeEmail(email);
        boolean authenticationChanged = !this.email.equalsIgnoreCase(normalizedEmail)
            || this.role != role;

        this.name = Objects.requireNonNull(name);
        this.email = normalizedEmail;
        this.role = Objects.requireNonNull(role);

        if (authenticationChanged) {
            authVersion++;
        }
    }

    void changeActiveStatus(boolean active) {
        if (this.active != active) {
            this.active = active;
            authVersion++;
        }
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
        authVersion++;
    }

    private static String normalizeEmail(String email) {
        return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public long getAuthVersion() {
        return authVersion;
    }
}
