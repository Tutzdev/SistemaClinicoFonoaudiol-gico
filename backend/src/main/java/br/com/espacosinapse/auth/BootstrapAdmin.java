package br.com.espacosinapse.auth;

import br.com.espacosinapse.common.DomainSupport;
import br.com.espacosinapse.users.AppUser;
import br.com.espacosinapse.users.AppUserRepository;
import br.com.espacosinapse.users.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private static final String EMAIL_PATTERN = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainSupport domainSupport;
    private final String email;
    private final String password;
    private final String name;

    public BootstrapAdmin(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        DomainSupport domainSupport,
        @Value("${app.bootstrap.email}") String email,
        @Value("${app.bootstrap.password}") String password,
        @Value("${app.bootstrap.name}") String name
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.domainSupport = domainSupport;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }

        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Configure e-mail e senha de bootstrap juntos.");
        }

        domainSupport.writeLock();
        if (userRepository.findByEmailIgnoreCase(email.strip()).isPresent()) {
            return;
        }

        UserService.validatePassword(password);

        if (!email.matches(EMAIL_PATTERN)) {
            throw new IllegalStateException("E-mail de bootstrap inválido.");
        }

        if (userRepository.count() > 0) {
            return;
        }

        AppUser user = new AppUser();
        user.name = name;
        user.email = email.strip().toLowerCase(Locale.ROOT);
        user.passwordHash = passwordEncoder.encode(password);
        user.role = AppUser.Role.ADMIN;

        userRepository.save(user);
        domainSupport.audit("BOOTSTRAPPED", "USER", user.id);
    }
}
