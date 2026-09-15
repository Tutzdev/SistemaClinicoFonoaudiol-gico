package br.com.espacosinapse.users;

import br.com.espacosinapse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {
    public enum Role {
        ADMIN,
        RECEPCAO
    }

    @Column(nullable = false, length = 160)
    public String name;

    @Column(nullable = false, length = 254, unique = true)
    public String email;

    @Column(nullable = false, length = 255)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Role role;

    public boolean active = true;

    public long authVersion;
}
