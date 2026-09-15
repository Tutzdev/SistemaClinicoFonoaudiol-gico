package br.com.espacosinapse.auth;

import br.com.espacosinapse.common.ApiDtos.AuthDto;
import br.com.espacosinapse.common.ApiDtos.LoginInput;
import br.com.espacosinapse.common.ApiDtos.PasswordInput;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import br.com.espacosinapse.users.AppUser;
import br.com.espacosinapse.users.AppUserRepository;
import br.com.espacosinapse.users.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final LoginThrottle loginThrottle;
    private final DomainSupport domainSupport;
    private final String dummyHash;

    public AuthController(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRepository csrfTokenRepository,
        LoginThrottle loginThrottle,
        DomainSupport domainSupport
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.loginThrottle = loginThrottle;
        this.domainSupport = domainSupport;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @GetMapping("/csrf")
    Map<String, String> csrf(@Parameter(hidden = true) CsrfToken token) {
        return Map.of(
            "token", token.getToken(),
            "headerName", token.getHeaderName()
        );
    }

    @PostMapping("/login")
    AuthDto login(
        @Valid @RequestBody LoginInput input,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String email = input.email().strip().toLowerCase(Locale.ROOT);
        loginThrottle.check(request.getRemoteAddr(), email);

        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String passwordHash = user == null ? dummyHash : user.passwordHash;
        boolean passwordMatches = passwordEncoder.matches(input.password(), passwordHash);
        if (user == null || !passwordMatches || !user.active) {
            throw invalidCredentials();
        }

        loginThrottle.success(email);
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        ClinicPrincipal principal = ClinicPrincipal.of(user);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.role.name()))
        );
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        csrfTokenRepository.saveToken(null, request, response);

        return new AuthDto(user.id, user.name, user.email, user.role);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(401, "E-mail ou senha inválidos.");
    }

    @GetMapping("/me")
    AuthDto me(@AuthenticationPrincipal ClinicPrincipal principal) {
        return new AuthDto(
            principal.id(),
            principal.name(),
            principal.email(),
            principal.role()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(
            request,
            response,
            SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @PutMapping("/password")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(
        @AuthenticationPrincipal ClinicPrincipal principal,
        @Valid @RequestBody PasswordInput input,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        domainSupport.writeLock();

        AppUser user = domainSupport.find(AppUser.class, principal.id());
        if (!passwordEncoder.matches(input.currentPassword(), user.passwordHash)) {
            throw ApiException.bad("A senha atual está incorreta.");
        }

        UserService.validatePassword(input.newPassword());
        user.passwordHash = passwordEncoder.encode(input.newPassword());
        user.authVersion++;
        domainSupport.audit("PASSWORD_CHANGED", "USER", user.id);

        // All sessions, including this one, expire. The user signs in with the new password.
        new SecurityContextLogoutHandler().logout(
            request,
            response,
            SecurityContextHolder.getContext().getAuthentication()
        );
    }
}
