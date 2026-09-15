package br.com.espacosinapse.appointments;

import br.com.espacosinapse.common.ApiDtos.AppointmentDto;
import br.com.espacosinapse.common.ApiDtos.AppointmentInput;
import br.com.espacosinapse.common.ApiDtos.HistoryDto;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.RescheduleInput;
import br.com.espacosinapse.common.ApiDtos.StatusInput;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import br.com.espacosinapse.patients.Patient;
import br.com.espacosinapse.professionals.AvailabilityService;
import br.com.espacosinapse.professionals.Professional;
import br.com.espacosinapse.services.ClinicService;
import br.com.espacosinapse.users.AppUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DomainSupport domainSupport;
    private final AvailabilityService availabilityService;

    public AppointmentService(
        AppointmentRepository appointmentRepository,
        DomainSupport domainSupport,
        AvailabilityService availabilityService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.domainSupport = domainSupport;
        this.availabilityService = availabilityService;
    }

    public AppointmentDto toDto(Appointment appointment) {
        Patient patient = domainSupport.find(Patient.class, appointment.patientId);
        Professional professional = domainSupport.find(Professional.class, appointment.professionalId);
        ClinicService service = domainSupport.find(ClinicService.class, appointment.serviceId);

        return new AppointmentDto(
            appointment.id,
            appointment.patientId,
            patient.name,
            appointment.professionalId,
            professional.name,
            appointment.serviceId,
            service.name,
            DomainSupport.offset(appointment.start),
            DomainSupport.offset(appointment.end),
            appointment.durationMinutes,
            appointment.status,
            appointment.version,
            appointment.createdAt,
            appointment.updatedAt
        );
    }

    public PageDto<AppointmentDto> list(
        OffsetDateTime from,
        OffsetDateTime to,
        UUID professionalId,
        UUID patientId,
        Appointment.Status status,
        int page,
        int size
    ) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw ApiException.bad("O início do filtro deve ser anterior ao fim.");
        }

        return PageDto.from(appointmentRepository.findAll((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("start"), from.toInstant()));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("start"), to.toInstant()));
            }
            if (professionalId != null) {
                predicates.add(builder.equal(root.get("professionalId"), professionalId));
            }
            if (patientId != null) {
                predicates.add(builder.equal(root.get("patientId"), patientId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        }, DomainSupport.page(page, size, "start")).map(this::toDto));
    }

    public AppointmentDto get(UUID id) {
        Appointment appointment = domainSupport.find(Appointment.class, id);

        return toDto(appointment);
    }

    @Transactional
    public AppointmentDto create(AppointmentInput input) {
        domainSupport.writeLock();

        Appointment appointment = new Appointment();
        appointment.patientId = input.patientId();
        appointment.professionalId = input.professionalId();
        appointment.serviceId = input.serviceId();
        appointment.createdBy = DomainSupport.actorId();

        ClinicService service = domainSupport.find(ClinicService.class, appointment.serviceId);
        appointment.durationMinutes = service.durationMinutes;
        appointment.start = input.start().toInstant();
        appointment.end = appointment.start.plusSeconds(appointment.durationMinutes * 60L);

        validate(appointment, null);
        domainSupport.entityManager.persist(appointment);
        recordHistory(appointment, "CREATED", null, null);
        domainSupport.audit("CREATED", "APPOINTMENT", appointment.id);
        domainSupport.entityManager.flush();

        return toDto(appointment);
    }

    @Transactional
    public AppointmentDto reschedule(UUID id, RescheduleInput input) {
        domainSupport.writeLock();

        Appointment appointment = domainSupport.find(Appointment.class, id);
        domainSupport.version(appointment, input.version());
        if (!appointment.isOpen()) {
            throw ApiException.conflict("Somente consultas agendadas ou confirmadas podem ser reagendadas.");
        }

        Instant previousStart = appointment.start;
        String previousStatus = appointment.status.name();

        appointment.start = input.start().toInstant();
        appointment.end = appointment.start.plusSeconds(appointment.durationMinutes * 60L);
        appointment.status = Appointment.Status.AGENDADO;

        validate(appointment, id);
        recordHistory(appointment, "RESCHEDULED", previousStart, previousStatus);
        domainSupport.audit("RESCHEDULED", "APPOINTMENT", id);
        domainSupport.entityManager.flush();

        return toDto(appointment);
    }

    @Transactional
    public AppointmentDto status(UUID id, StatusInput input) {
        domainSupport.writeLock();

        Appointment appointment = domainSupport.find(Appointment.class, id);
        domainSupport.version(appointment, input.version());
        if (!appointment.isOpen()) {
            throw ApiException.conflict("O estado desta consulta é final e não pode ser alterado.");
        }
        if (input.status() == Appointment.Status.AGENDADO || input.status() == appointment.status) {
            throw ApiException.conflict("Transição de status inválida.");
        }

        boolean finishingAppointment = input.status() == Appointment.Status.CONCLUIDO
            || input.status() == Appointment.Status.NAO_COMPARECEU;
        if (finishingAppointment && appointment.end.isAfter(Instant.now())) {
            throw ApiException.conflict(
                "Aguarde o horário de término para concluir ou marcar não comparecimento."
            );
        }

        String previousStatus = appointment.status.name();
        appointment.status = input.status();

        recordHistory(appointment, "STATUS_CHANGED", appointment.start, previousStatus);
        domainSupport.audit("STATUS_CHANGED", "APPOINTMENT", id);
        domainSupport.entityManager.flush();

        return toDto(appointment);
    }

    private void validate(Appointment appointment, UUID ignoredAppointmentId) {
        if (!appointment.start.isAfter(Instant.now())) {
            throw ApiException.bad("O agendamento precisa começar no futuro.");
        }

        Patient patient = domainSupport.find(Patient.class, appointment.patientId);
        if (!patient.active) {
            throw ApiException.conflict("O paciente está inativo.");
        }

        Professional professional = domainSupport.find(Professional.class, appointment.professionalId);
        if (!professional.active) {
            throw ApiException.conflict("O profissional está inativo.");
        }

        ClinicService service = domainSupport.find(ClinicService.class, appointment.serviceId);
        if (!service.active) {
            throw ApiException.conflict("O serviço está inativo.");
        }
        if (!professional.serviceIds.contains(appointment.serviceId)) {
            throw ApiException.conflict("O profissional não está vinculado a este serviço.");
        }

        availabilityService.validateSlot(appointment.professionalId, appointment.start, appointment.end);
        validateConflicts(appointment, ignoredAppointmentId);
    }

    private void validateConflicts(Appointment appointment, UUID ignoredAppointmentId) {
        String query = "select a from Appointment a "
            + "where a.status<>:cancelled "
            + "and (a.professionalId=:professional or a.patientId=:patient) "
            + "and a.start<:end and a.end>:start";
        if (ignoredAppointmentId != null) {
            query += " and a.id<>:ignore";
        }

        var conflictQuery = domainSupport.entityManager
            .createQuery(query, Appointment.class)
            .setParameter("cancelled", Appointment.Status.CANCELADO)
            .setParameter("professional", appointment.professionalId)
            .setParameter("patient", appointment.patientId)
            .setParameter("start", appointment.start)
            .setParameter("end", appointment.end);
        if (ignoredAppointmentId != null) {
            conflictQuery.setParameter("ignore", ignoredAppointmentId);
        }

        List<Appointment> conflicts = conflictQuery.getResultList();
        if (!conflicts.isEmpty()) {
            throw DomainSupport.conflicts(
                "O profissional ou paciente já tem uma consulta neste intervalo.",
                conflicts
            );
        }
    }

    private void recordHistory(
        Appointment appointment,
        String action,
        Instant previousStart,
        String previousStatus
    ) {
        AppointmentHistory history = new AppointmentHistory();
        history.appointmentId = appointment.id;
        history.actorId = DomainSupport.actorId();
        history.action = action;
        history.previousStart = previousStart;
        history.newStart = appointment.start;
        history.previousStatus = previousStatus;
        history.newStatus = appointment.status.name();
        domainSupport.entityManager.persist(history);
    }

    public List<HistoryDto> history(UUID id) {
        domainSupport.find(Appointment.class, id);

        return domainSupport.entityManager
            .createQuery(
                "select h from AppointmentHistory h where h.appointmentId=:id order by h.createdAt",
                AppointmentHistory.class
            )
            .setParameter("id", id)
            .getResultList()
            .stream()
            .map(history -> new HistoryDto(
                history.id,
                history.action,
                domainSupport.find(AppUser.class, history.actorId).name,
                DomainSupport.offset(history.createdAt),
                DomainSupport.offset(history.previousStart),
                DomainSupport.offset(history.newStart),
                history.previousStatus,
                history.newStatus
            ))
            .toList();
    }
}
