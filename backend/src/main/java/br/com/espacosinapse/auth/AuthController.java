package br.com.espacosinapse.auth;

import br.com.espacosinapse.common.ApiDtos.AuthDto;
import br.com.espacosinapse.common.ApiDtos.LoginInput;
import br.com.espacosinapse.common.ApiDtos.PasswordInput;
import br.com.espacosinapse.users.AppUser;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(
        AuthService authService,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRepository csrfTokenRepository
    ) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
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
        AppUser user = authService.authenticate(input, request.getRemoteAddr());
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        ClinicPrincipal principal = ClinicPrincipal.of(user);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        csrfTokenRepository.saveToken(null, request, response);

        return new AuthDto(user.getId(), user.getName(), user.getEmail(), user.getRole());
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(
        @AuthenticationPrincipal ClinicPrincipal principal,
        @Valid @RequestBody PasswordInput input,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        authService.changePassword(principal.id(), input);

        // All sessions, including this one, expire. The user signs in with the new password.
        new SecurityContextLogoutHandler().logout(
            request,
            response,
            SecurityContextHolder.getContext().getAuthentication()
        );
    }
}
