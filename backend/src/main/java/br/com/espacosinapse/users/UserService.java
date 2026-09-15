package br.com.espacosinapse.users;

import br.com.espacosinapse.common.ApiDtos.ActiveInput;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.UserDto;
import br.com.espacosinapse.common.ApiDtos.UserInput;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final AppUserRepository userRepository;
    private final DomainSupport domainSupport;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        AppUserRepository userRepository,
        DomainSupport domainSupport,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.domainSupport = domainSupport;
        this.passwordEncoder = passwordEncoder;
    }

    public static UserDto toDto(AppUser user) {
        return new UserDto(
            user.id,
            user.name,
            user.email,
            user.role,
            user.active,
            user.version,
            user.createdAt,
            user.updatedAt
        );
    }

    public PageDto<UserDto> list(String search, int page, int size) {
        String term = search.toLowerCase(Locale.ROOT);

        return PageDto.from(userRepository.findAll(
            (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), "%" + term + "%"),
                builder.like(builder.lower(root.get("email")), "%" + term + "%")
            ),
            DomainSupport.page(page, size, "name")
        ).map(UserService::toDto));
    }

    public UserDto get(UUID id) {
        AppUser user = domainSupport.find(AppUser.class, id);

        return toDto(user);
    }

    public static void validatePassword(String value) {
        if (value == null || value.length() < 12 || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.bad("A senha precisa ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8.");
        }
    }

    @Transactional
    public UserDto create(UserInput input) {
        domainSupport.writeLock();
        validatePassword(input.password());

        AppUser user = new AppUser();
        apply(user, input);
        user.passwordHash = passwordEncoder.encode(input.password());

        domainSupport.entityManager.persist(user);
        domainSupport.audit("CREATED", "USER", user.id);
        domainSupport.entityManager.flush();

        return toDto(user);
    }

    @Transactional
    public UserDto update(UUID id, UserInput input) {
        domainSupport.writeLock();

        AppUser user = domainSupport.find(AppUser.class, id);
        domainSupport.version(user, input.version());
        if (input.password() != null) {
            throw ApiException.bad("Use a alteração de senha autenticada para trocar a senha.");
        }
        if (user.role == AppUser.Role.ADMIN && input.role() != AppUser.Role.ADMIN && user.active) {
            requireAnotherActiveAdmin();
        }

        boolean invalidateSessions = !user.email.equalsIgnoreCase(input.email().strip())
            || user.role != input.role();
        apply(user, input);
        if (invalidateSessions) {
            user.authVersion++;
        }

        domainSupport.audit("UPDATED", "USER", id);
        domainSupport.entityManager.flush();

        return toDto(user);
    }

    private void apply(AppUser user, UserInput input) {
        user.name = input.name().strip();
        user.email = input.email().strip().toLowerCase(Locale.ROOT);
        user.role = input.role();
    }

    private void requireAnotherActiveAdmin() {
        Long activeAdmins = domainSupport.entityManager
            .createQuery(
                "select count(u) from AppUser u where u.active=true and u.role=:role",
                Long.class
            )
            .setParameter("role", AppUser.Role.ADMIN)
            .getSingleResult();
        if (activeAdmins <= 1) {
            throw ApiException.conflict("A clínica precisa manter ao menos um administrador ativo.");
        }
    }

    @Transactional
    public UserDto active(UUID id, ActiveInput input) {
        domainSupport.writeLock();

        AppUser user = domainSupport.find(AppUser.class, id);
        domainSupport.version(user, input.version());
        if (!input.active() && user.active && user.role == AppUser.Role.ADMIN) {
            requireAnotherActiveAdmin();
        }
        if (user.active != input.active()) {
            user.active = input.active();
            user.authVersion++;
        }

        domainSupport.audit(input.active() ? "ACTIVATED" : "INACTIVATED", "USER", id);
        domainSupport.entityManager.flush();

        return toDto(user);
    }

    @Transactional
    public void delete(UUID id) {
        domainSupport.writeLock();

        AppUser user = domainSupport.find(AppUser.class, id);
        if (id.equals(DomainSupport.actorId())) {
            throw ApiException.conflict("Você não pode excluir seu próprio usuário.");
        }
        if (user.active && user.role == AppUser.Role.ADMIN) {
            requireAnotherActiveAdmin();
        }

        domainSupport.entityManager.remove(user);
        domainSupport.audit("DELETED", "USER", id);
        domainSupport.entityManager.flush();
    }
}
