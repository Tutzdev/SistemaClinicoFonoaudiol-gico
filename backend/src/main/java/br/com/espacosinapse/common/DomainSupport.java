package br.com.espacosinapse.common;

import br.com.espacosinapse.appointments.Appointment;
import br.com.espacosinapse.auth.ClinicPrincipal;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DomainSupport {
    public static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");

    private static final long WRITE_LOCK_ID = 65952229L;
    private static final Set<String> APPOINTMENT_REFERENCE_FIELDS = Set.of(
        "patientId",
        "professionalId",
        "serviceId"
    );

    public final EntityManager entityManager;

    public DomainSupport(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // All administrative writes share this transaction-scoped lock. At a single clinic's
    // volume, serializing writes gives a simple, correct boundary across scheduling,
    // availability, user permissions and inactivation. Reads stay concurrent.
    public void writeLock() {
        entityManager
            .createNativeQuery("SELECT pg_advisory_xact_lock(" + WRITE_LOCK_ID + ")")
            .getSingleResult();
    }

    public <T> T find(Class<T> type, UUID id) {
        T value = entityManager.find(type, id);
        if (value == null) {
            throw ApiException.missing();
        }

        return value;
    }

    public void version(BaseEntity entity, Long expected) {
        if (expected == null) {
            throw ApiException.bad("Informe a versão do registro para atualizar.");
        }
        if (entity.getVersion() != expected) {
            throw ApiException.conflict(
                "Este registro foi atualizado por outra pessoa. Recarregue antes de salvar."
            );
        }
    }

    public static PageRequest page(int number, int size, String sort) {
        if (number < 0 || size < 1 || size > 100) {
            throw ApiException.bad("Página deve ser positiva e o tamanho deve estar entre 1 e 100.");
        }

        return PageRequest.of(number, size, Sort.by(sort));
    }

    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public static void phone(String phone, boolean required) {
        if (clean(phone) == null) {
            if (required) {
                throw ApiException.bad("Informe um telefone de contato.");
            }
            return;
        }

        int digitCount = phone.replaceAll("\\D", "").length();
        if (!phone.matches("[+()0-9 .-]+") || digitCount < 10 || digitCount > 15) {
            throw ApiException.bad("Telefone inválido. Inclua o DDD.");
        }
    }

    public static UUID actorId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ClinicPrincipal principal) {
            return principal.id();
        }

        return null;
    }

    public void audit(String action, String type, UUID id) {
        AuditEvent event = new AuditEvent(actorId(), action, type, id);
        entityManager.persist(event);
    }

    public List<Appointment> openFuture(String field, UUID id) {
        validateAppointmentReferenceField(field);

        return entityManager
            .createQuery(
                "select a from Appointment a where a." + field
                    + "=:id and a.end>:now and a.status in (:statuses)",
                Appointment.class
            )
            .setParameter("id", id)
            .setParameter("now", Instant.now())
            .setParameter(
                "statuses",
                List.of(Appointment.Status.AGENDADO, Appointment.Status.CONFIRMADO)
            )
            .getResultList();
    }

    public void requireNoFuture(String field, UUID id) {
        List<Appointment> conflicts = openFuture(field, id);
        if (!conflicts.isEmpty()) {
            throw conflicts(
                "Resolva os agendamentos futuros antes de inativar este cadastro.",
                conflicts
            );
        }
    }

    public void requireNoHistory(String field, UUID id) {
        validateAppointmentReferenceField(field);

        long count = entityManager
            .createQuery("select count(a) from Appointment a where a." + field + "=:id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
        if (count > 0) {
            throw ApiException.conflict(
                "Este cadastro possui histórico de consultas. Use a inativação para preservar os registros."
            );
        }
    }

    private static void validateAppointmentReferenceField(String field) {
        if (!APPOINTMENT_REFERENCE_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unsupported field");
        }
    }

    public static ApiException conflicts(String message, List<Appointment> conflicts) {
        List<UUID> appointmentIds = conflicts.stream()
            .map(Appointment::getId)
            .toList();

        return new ApiException(409, message, appointmentIds);
    }

    public static OffsetDateTime offset(Instant value) {
        return value == null ? null : value.atZone(CLINIC_ZONE).toOffsetDateTime();
    }
}
