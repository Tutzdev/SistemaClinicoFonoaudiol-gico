package br.com.espacosinapse;

import br.com.espacosinapse.auth.ClinicPrincipal;
import br.com.espacosinapse.users.AppUser;
import br.com.espacosinapse.users.AppUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.bootstrap.email=",
    "app.bootstrap.password=",
    "spring.profiles.active=test",
    "debug=false",
    "logging.level.org.springframework=INFO"
})
@AutoConfigureMockMvc
class ClinicIntegrationTest {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String PASSWORD = "Integration-test-9!";

    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String testDatabaseUrl = System.getenv("TEST_DB_URL");
        if (testDatabaseUrl != null && !testDatabaseUrl.isBlank()) {
            // Only a dedicated test database may be reset by this suite.
            if (!testDatabaseUrl.matches(".*[/]sinapse_test(?:[?].*)?")) {
                throw new IllegalStateException("TEST_DB_URL precisa apontar para sinapse_test.");
            }

            properties.add("spring.datasource.url", () -> testDatabaseUrl);
            properties.add("spring.datasource.username", () -> System.getenv("TEST_DB_USERNAME"));
            properties.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
            return;
        }

        postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("sinapse_test");
        postgres.start();
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private Authentication admin;
    private Authentication reception;
    private UUID adminId;
    private UUID receptionId;
    private OffsetDateTime slot;

    @Autowired
    ClinicIntegrationTest(
        MockMvc mvc,
        ObjectMapper objectMapper,
        JdbcTemplate jdbcTemplate,
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.mvc = mvc;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void setup() {
        jdbcTemplate.execute(
            "TRUNCATE appointment_history, audit_events, appointments, availability_blocks, "
                + "availability_periods, professional_services, patients, professionals, "
                + "clinic_services, app_users CASCADE"
        );
        jdbcTemplate.update(
            "update clinic_settings set public_address=null,address_confirmed=false,version=0 "
                + "where id='00000000-0000-0000-0000-000000000001'"
        );

        AppUser adminUser = new AppUser(
            "Admin de teste",
            "admin@test.invalid",
            passwordEncoder.encode(PASSWORD),
            AppUser.Role.ADMIN
        );
        userRepository.saveAndFlush(adminUser);
        adminId = adminUser.getId();
        admin = authenticationFor(adminUser);

        AppUser receptionUser = new AppUser(
            "Recepção de teste",
            "recepcao@test.invalid",
            adminUser.getPasswordHash(),
            AppUser.Role.RECEPCAO
        );
        userRepository.saveAndFlush(receptionUser);
        receptionId = receptionUser.getId();
        reception = authenticationFor(receptionUser);

        slot = LocalDate.now(CLINIC_ZONE)
            .plusDays(2)
            .atTime(9, 0)
            .atZone(CLINIC_ZONE)
            .toOffsetDateTime();
    }

    private Authentication authenticationFor(AppUser user) {
        ClinicPrincipal principal = ClinicPrincipal.of(user);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    private ResultActions performRequest(
        String method,
        String path,
        Object body,
        Authentication actor
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = request(
            HttpMethod.valueOf(method),
            "/api/v1" + path
        ).with(authentication(actor)).with(csrf());
        if (body != null) {
            requestBuilder
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body));
        }

        return mvc.perform(requestBuilder);
    }

    private JsonNode created(String path, Object body) throws Exception {
        String response = performRequest("POST", path, body, admin)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response);
    }

    private UUID patient(String name) throws Exception {
        JsonNode patient = created("/patients", Map.of(
            "name", name,
            "birthDate", "1990-01-01",
            "phone", "61999990000"
        ));

        return UUID.fromString(patient.get("id").asText());
    }

    private UUID service() throws Exception {
        JsonNode service = created("/services", Map.of(
            "name", "Serviço de teste",
            "durationMinutes", 45,
            "published", false
        ));

        return UUID.fromString(service.get("id").asText());
    }

    private UUID professional(UUID serviceId) throws Exception {
        JsonNode professional = created("/professionals", Map.of(
            "name", "Profissional de teste",
            "serviceIds", List.of(serviceId),
            "published", false
        ));
        UUID professionalId = UUID.fromString(professional.get("id").asText());

        List<Object> periods = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            periods.add(Map.of(
                "dayOfWeek", day,
                "startTime", "08:00",
                "endTime", "18:00"
            ));
        }
        performRequest(
            "PUT",
            "/professionals/" + professionalId + "/availability",
            Map.of("periods", periods),
            admin
        ).andExpect(status().isOk());

        return professionalId;
    }

    private Map<String, Object> booking(
        UUID patientId,
        UUID professionalId,
        UUID serviceId,
        OffsetDateTime start
    ) {
        return Map.of(
            "patientId", patientId,
            "professionalId", professionalId,
            "serviceId", serviceId,
            "start", start.toString()
        );
    }

    private JsonNode book(
        UUID patientId,
        UUID professionalId,
        UUID serviceId,
        OffsetDateTime start
    ) throws Exception {
        return created("/appointments", booking(patientId, professionalId, serviceId, start));
    }

    @Test
    void sessionLoginCsrfLogoutAndDeactivation() throws Exception {
        mvc.perform(get("/api/v1/patients"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "admin@test.invalid",
                    "password", PASSWORD
                ))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "admin@test.invalid",
                    "password", "wrong"
                ))))
            .andExpect(status().isUnauthorized());

        var loginResult = mvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "recepcao@test.invalid",
                    "password", PASSWORD
                ))))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("RECEPCAO"));
        performRequest(
            "PATCH",
            "/users/" + receptionId + "/active",
            Map.of("active", false, "version", 0),
            admin
        ).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout")
                .with(authentication(admin))
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void receptionCannotModifyAdministrativeResources() throws Exception {
        performRequest("GET", "/users", null, reception)
            .andExpect(status().isForbidden());
        performRequest("GET", "/clinic-settings", null, reception)
            .andExpect(status().isForbidden());
        performRequest(
            "POST",
            "/services",
            Map.of("name", "X", "durationMinutes", 30),
            reception
        ).andExpect(status().isForbidden());
        performRequest(
            "POST",
            "/patients",
            Map.of(
                "name", "Paciente recepção",
                "birthDate", "1990-01-01",
                "phone", "61999990000"
            ),
            reception
        ).andExpect(status().isCreated());
        performRequest(
            "POST",
            "/patients",
            Map.of(
                "name", "X",
                "birthDate", "1990-01-01",
                "phone", "61999990000",
                "role", "ADMIN"
            ),
            admin
        ).andExpect(status().isBadRequest());
    }

    @Test
    void validatesMinorsAndRejectsStaleUpdates() throws Exception {
        performRequest(
            "POST",
            "/patients",
            Map.of(
                "name", "Criança teste",
                "birthDate", LocalDate.now().minusYears(5).toString(),
                "phone", "61999990000"
            ),
            admin
        ).andExpect(status().isBadRequest());
        performRequest(
            "POST",
            "/patients",
            Map.of(
                "name", "Criança teste",
                "birthDate", LocalDate.now().minusYears(5).toString(),
                "phone", "61999990000",
                "guardianName", "Responsável teste",
                "guardianRelationship", "Mãe",
                "guardianPhone", "61999990000"
            ),
            admin
        ).andExpect(status().isCreated());
        performRequest(
            "POST",
            "/patients",
            Map.of(
                "name", "Futuro",
                "birthDate", LocalDate.now().plusDays(1).toString(),
                "phone", "61999990000"
            ),
            admin
        ).andExpect(status().isBadRequest());

        UUID patientId = patient("Inicial");
        Map<String, Object> update = Map.of(
            "name", "Atualizado",
            "birthDate", "1990-01-01",
            "phone", "61999990000",
            "version", 0
        );
        performRequest("PUT", "/patients/" + patientId, update, admin)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));
        performRequest("PUT", "/patients/" + patientId, update, admin)
            .andExpect(status().isConflict());
        performRequest("GET", "/patients?search=Atualizado&page=0&size=20", null, admin)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void validatesAvailabilityBlocksAndBackToBackAppointments() throws Exception {
        UUID serviceId = service();
        UUID professionalId = professional(serviceId);
        UUID patientId = patient("Paciente um");

        performRequest(
            "POST",
            "/appointments",
            booking(patientId, professionalId, serviceId, slot.withHour(7)),
            admin
        ).andExpect(status().isConflict());
        book(patientId, professionalId, serviceId, slot);
        performRequest(
            "POST",
            "/appointments",
            booking(patientId, professionalId, serviceId, slot.plusMinutes(30)),
            admin
        ).andExpect(status().isConflict());
        book(patientId, professionalId, serviceId, slot.plusMinutes(45));

        performRequest(
            "POST",
            "/professionals/" + professionalId + "/blocks",
            Map.of(
                "start", slot.plusHours(2).toString(),
                "end", slot.plusHours(3).toString()
            ),
            admin
        ).andExpect(status().isCreated());
        performRequest(
            "POST",
            "/appointments",
            booking(patientId, professionalId, serviceId, slot.plusHours(2)),
            admin
        ).andExpect(status().isConflict());
        performRequest(
            "PUT",
            "/professionals/" + professionalId + "/availability",
            Map.of("periods", Arrays.asList((Object) null)),
            admin
        ).andExpect(status().isBadRequest());

        UUID otherProfessionalId = professional(serviceId);
        performRequest(
            "POST",
            "/appointments",
            booking(patientId, otherProfessionalId, serviceId, slot),
            admin
        ).andExpect(status().isConflict());
    }

    @Test
    void onlyOneConcurrentReservationSucceeds() throws Exception {
        UUID serviceId = service();
        UUID professionalId = professional(serviceId);
        UUID patientId = patient("Concorrente");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> attempt = () -> {
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);

                return performRequest(
                    "POST",
                    "/appointments",
                    booking(patientId, professionalId, serviceId, slot),
                    admin
                ).andReturn().getResponse().getStatus();
            };
            var firstAttempt = pool.submit(attempt);
            var secondAttempt = pool.submit(attempt);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(List.of(
                firstAttempt.get(20, TimeUnit.SECONDS),
                secondAttempt.get(20, TimeUnit.SECONDS)
            )).containsExactlyInAnyOrder(201, 409);
        }

        assertThat(jdbcTemplate.queryForObject("select count(*) from appointments", Integer.class))
            .isEqualTo(1);
    }

    @Test
    void postgresRejectsOverlapEvenOutsideTheApplication() throws Exception {
        UUID serviceId = service();
        UUID professionalId = professional(serviceId);
        UUID patientId = patient("Restrição PostgreSQL");
        book(patientId, professionalId, serviceId, slot);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "insert into appointments("
                + "id,version,created_at,updated_at,patient_id,professional_id,service_id,"
                + "created_by,starts_at,ends_at,duration_minutes,status"
                + ") values(?,0,now(),now(),?,?,?,?,?,?,45,'AGENDADO')",
            UUID.randomUUID(),
            patientId,
            professionalId,
            serviceId,
            adminId,
            slot,
            slot.plusMinutes(45)
        )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void futureAppointmentsPreventDestructiveConfigurationChanges() throws Exception {
        UUID serviceId = service();
        UUID professionalId = professional(serviceId);
        UUID patientId = patient("Com vínculo");
        JsonNode appointment = book(patientId, professionalId, serviceId, slot);

        for (String path : List.of(
            "/patients/" + patientId,
            "/professionals/" + professionalId,
            "/services/" + serviceId
        )) {
            performRequest(
                "PATCH",
                path + "/active",
                Map.of("active", false, "version", 0),
                admin
            ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflictingAppointmentIds[0]")
                    .value(appointment.get("id").asText()));
            performRequest("DELETE", path, null, admin)
                .andExpect(status().isConflict());
        }

        performRequest(
            "PUT",
            "/professionals/" + professionalId + "/availability",
            Map.of("periods", List.of()),
            admin
        ).andExpect(status().isConflict());
        performRequest(
            "POST",
            "/professionals/" + professionalId + "/blocks",
            Map.of("start", slot.toString(), "end", slot.plusHours(1).toString()),
            admin
        ).andExpect(status().isConflict());
        performRequest(
            "PUT",
            "/professionals/" + professionalId,
            Map.of(
                "name", "Profissional",
                "serviceIds", List.of(),
                "published", false,
                "version", 0
            ),
            admin
        ).andExpect(status().isConflict());
    }

    @Test
    void reschedulingKeepsHistoryAndCancellingFreesSlot() throws Exception {
        UUID serviceId = service();
        UUID professionalId = professional(serviceId);
        UUID patientId = patient("Histórico");
        String appointmentId = book(patientId, professionalId, serviceId, slot).get("id").asText();

        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/status",
            Map.of("status", "CONCLUIDO", "version", 0),
            admin
        ).andExpect(status().isConflict());
        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/status",
            Map.of("status", "CONFIRMADO", "version", 0),
            admin
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));
        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/reschedule",
            Map.of("start", slot.plusHours(2).toString(), "version", 1),
            admin
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AGENDADO"));
        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/reschedule",
            Map.of("start", slot.plusHours(3).toString(), "version", 1),
            admin
        ).andExpect(status().isConflict());
        performRequest("GET", "/appointments/" + appointmentId + "/history", null, admin)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[2].previousStatus").value("CONFIRMADO"));
        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/status",
            Map.of("status", "CANCELADO", "version", 2),
            admin
        ).andExpect(status().isOk());

        book(patientId, professionalId, serviceId, slot.plusHours(2));
        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/status",
            Map.of("status", "CONFIRMADO", "version", 3),
            admin
        ).andExpect(status().isConflict());
    }

    @Test
    void completionAfterEndIsFinalAndDurationIsSnapshot() throws Exception {
        UUID serviceId = service();
        UUID professionalId = professional(serviceId);
        UUID patientId = patient("Conclusão");
        String appointmentId = book(patientId, professionalId, serviceId, slot).get("id").asText();

        performRequest(
            "PUT",
            "/services/" + serviceId,
            Map.of(
                "name", "Novo tempo",
                "durationMinutes", 60,
                "published", false,
                "version", 0
            ),
            admin
        ).andExpect(status().isOk());
        performRequest("GET", "/appointments/" + appointmentId, null, admin)
            .andExpect(jsonPath("$.durationMinutes").value(45));

        jdbcTemplate.update(
            "update appointments set starts_at=now()-interval '2 hours',"
                + "ends_at=now()-interval '75 minutes' where id=?",
            UUID.fromString(appointmentId)
        );
        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/status",
            Map.of("status", "CONCLUIDO", "version", 0),
            admin
        ).andExpect(status().isOk());
        performRequest(
            "PATCH",
            "/appointments/" + appointmentId + "/reschedule",
            Map.of("start", slot.toString(), "version", 1),
            admin
        ).andExpect(status().isConflict());
        performRequest(
            "PATCH",
            "/patients/" + patientId + "/active",
            Map.of("active", false, "version", 0),
            admin
        ).andExpect(status().isOk());
        performRequest("DELETE", "/patients/" + patientId, null, admin)
            .andExpect(status().isConflict());
    }

    @Test
    void publicApiDoesNotExposePrivateDataOrUnconfirmedAddress() throws Exception {
        UUID serviceId = service();
        UUID professionalId = professional(serviceId);
        patient("Paciente privado");

        mvc.perform(get("/api/v1/public/clinic"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicAddress").isEmpty())
            .andExpect(jsonPath("$.addressConfirmed").value(false));
        mvc.perform(get("/api/v1/public/professionals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        performRequest(
            "PUT",
            "/professionals/" + professionalId,
            Map.of(
                "name", "Profissional publicado",
                "email", "private@test.invalid",
                "phone", "61999990000",
                "registration", "CRFA-DEMO",
                "region", "1",
                "bio", "Texto aprovado",
                "serviceIds", List.of(serviceId),
                "published", true,
                "version", 0
            ),
            admin
        ).andExpect(status().isOk());

        mvc.perform(get("/api/v1/public/professionals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Profissional publicado"))
            .andExpect(jsonPath("$[0].email").doesNotExist())
            .andExpect(jsonPath("$[0].phone").doesNotExist());
        mvc.perform(get("/api/v1/public/patients"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/appointments"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void lastAdminAndPasswordChangeAreProtected() throws Exception {
        performRequest(
            "PATCH",
            "/users/" + adminId + "/active",
            Map.of("active", false, "version", 0),
            admin
        ).andExpect(status().isConflict());
        performRequest(
            "PUT",
            "/users/" + adminId,
            Map.of(
                "name", "Admin",
                "email", "admin@test.invalid",
                "role", "RECEPCAO",
                "version", 0
            ),
            admin
        ).andExpect(status().isConflict());
        performRequest(
            "PUT",
            "/auth/password",
            Map.of(
                "currentPassword", "wrong",
                "newPassword", "Another-password-7!"
            ),
            admin
        ).andExpect(status().isBadRequest());
        performRequest(
            "PUT",
            "/auth/password",
            Map.of(
                "currentPassword", PASSWORD,
                "newPassword", "Another-password-7!"
            ),
            admin
        ).andExpect(status().isNoContent());
        performRequest("GET", "/auth/me", null, admin)
            .andExpect(status().isUnauthorized());
    }

    @Test
    void independentRecordsCanBeDeletedAndServicePublicationIsExplicit() throws Exception {
        UUID patientId = patient("Removível");
        performRequest("DELETE", "/patients/" + patientId, null, admin)
            .andExpect(status().isNoContent());
        performRequest("GET", "/patients/" + patientId, null, admin)
            .andExpect(status().isNotFound());

        UUID serviceId = service();
        mvc.perform(get("/api/v1/public/services"))
            .andExpect(jsonPath("$.length()").value(0));
        performRequest(
            "PUT",
            "/services/" + serviceId,
            Map.of(
                "name", "Aprovado",
                "description", "Descrição aprovada",
                "durationMinutes", 45,
                "published", true,
                "version", 0
            ),
            admin
        ).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/services"))
            .andExpect(jsonPath("$[0].name").value("Aprovado"))
            .andExpect(jsonPath("$[0].version").doesNotExist());
        performRequest("DELETE", "/services/" + serviceId, null, admin)
            .andExpect(status().isNoContent());
    }
}
