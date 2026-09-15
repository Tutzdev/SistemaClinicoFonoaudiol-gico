package br.com.espacosinapse.auth;

import br.com.espacosinapse.users.AppUser;

import java.io.Serializable;
import java.util.UUID;

public record ClinicPrincipal(
    UUID id,
    String name,
    String email,
    AppUser.Role role,
    long authVersion
) implements Serializable {
    public static ClinicPrincipal of(AppUser user) {
        return new ClinicPrincipal(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getAuthVersion()
        );
    }
}
