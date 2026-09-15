package br.com.espacosinapse.auth;

import br.com.espacosinapse.users.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AppUserRepository userRepository,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        http
            .securityContext(configurer ->
                configurer.securityContextRepository(securityContextRepository)
            )
            .csrf(configurer -> configurer.csrfTokenRepository(csrfTokenRepository))
            .formLogin(configurer -> configurer.disable())
            .httpBasic(configurer -> configurer.disable())
            .logout(configurer -> configurer.disable())
            .requestCache(configurer -> configurer.disable())
            .authorizeHttpRequests(configurer -> configurer
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/public/**",
                    "/api/v1/auth/csrf",
                    "/api/v1/health"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/**", "/swagger-ui/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(configurer -> configurer
                .authenticationEntryPoint((request, response, exception) ->
                    problem(response, 401, "Sua sessão expirou ou você precisa entrar.")
                )
                .accessDeniedHandler((request, response, exception) ->
                    problem(
                        response,
                        403,
                        "Acesso negado ou token de segurança inválido. Atualize a página."
                    )
                ))
            .addFilterAfter(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
                ) throws ServletException, IOException {
                    var authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null
                        && authentication.getPrincipal() instanceof ClinicPrincipal principal) {
                        var user = userRepository.findById(principal.id()).orElse(null);
                        if (user == null || !user.active || user.authVersion != principal.authVersion()) {
                            SecurityContextHolder.clearContext();
                            var session = request.getSession(false);
                            if (session != null) {
                                session.invalidate();
                            }
                        } else {
                            var refreshedAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                                ClinicPrincipal.of(user),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.role.name()))
                            );
                            SecurityContextHolder.getContext().setAuthentication(refreshedAuthentication);
                        }
                    }

                    filterChain.doFilter(request, response);
                }
            }, SecurityContextHolderFilter.class);

        return http.build();
    }

    public static void problem(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"type\":\"about:blank\",\"status\":" + status
                + ",\"title\":\"Acesso\",\"detail\":\"" + detail + "\"}"
        );
    }
}
