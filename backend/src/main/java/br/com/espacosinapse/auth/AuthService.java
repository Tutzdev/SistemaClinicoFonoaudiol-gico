package br.com.espacosinapse.auth;

import br.com.espacosinapse.common.ApiDtos.LoginInput;
import br.com.espacosinapse.common.ApiDtos.PasswordInput;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import br.com.espacosinapse.users.AppUser;
import br.com.espacosinapse.users.AppUserRepository;
import br.com.espacosinapse.users.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginThrottle loginThrottle;
    private final DomainSupport domainSupport;
    private final String dummyPasswordHash;

    public AuthService(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        LoginThrottle loginThrottle,
        DomainSupport domainSupport
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginThrottle = loginThrottle;
        this.domainSupport = domainSupport;
        dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AppUser authenticate(LoginInput input, String remoteAddress) {
        String email = input.email().strip().toLowerCase(Locale.ROOT);
        loginThrottle.check(remoteAddress, email);

        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String passwordHash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(input.password(), passwordHash);
        if (user == null || !passwordMatches || !user.isActive()) {
            throw invalidCredentials();
        }

        loginThrottle.success(email);
        return user;
    }

    @Transactional
    public void changePassword(UUID userId, PasswordInput input) {
        domainSupport.writeLock();

        AppUser user = domainSupport.find(AppUser.class, userId);
        if (!passwordEncoder.matches(input.currentPassword(), user.getPasswordHash())) {
            throw ApiException.bad("A senha atual está incorreta.");
        }

        UserService.validatePassword(input.newPassword());
        user.changePassword(passwordEncoder.encode(input.newPassword()));
        domainSupport.audit("PASSWORD_CHANGED", "USER", user.getId());
    }

    private static ApiException invalidCredentials() {
        return new ApiException(401, "E-mail ou senha inválidos.");
    }
}
