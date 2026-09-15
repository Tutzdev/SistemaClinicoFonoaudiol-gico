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

    private AppointmentDto toDto(Appointment appointment) {
        Patient patient = domainSupport.find(Patient.class, appointment.getPatientId());
        Professional professional = domainSupport.find(Professional.class, appointment.getProfessionalId());
        ClinicService service = domainSupport.find(ClinicService.class, appointment.getServiceId());

        return new AppointmentDto(
            appointment.getId(),
            appointment.getPatientId(),
            patient.getName(),
            appointment.getProfessionalId(),
            professional.getName(),
            appointment.getServiceId(),
            service.getName(),
            DomainSupport.offset(appointment.getStart()),
            DomainSupport.offset(appointment.getEnd()),
            appointment.getDurationMinutes(),
            appointment.getStatus(),
            appointment.getVersion(),
            appointment.getCreatedAt(),
            appointment.getUpdatedAt()
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

        ClinicService service = domainSupport.find(ClinicService.class, input.serviceId());
        Appointment appointment = new Appointment(
            input.patientId(),
            input.professionalId(),
            input.serviceId(),
            DomainSupport.actorId(),
            input.start().toInstant(),
            service.getDurationMinutes()
        );

        validate(appointment, null);
        domainSupport.entityManager.persist(appointment);
        recordHistory(appointment, "CREATED", null, null);
        domainSupport.audit("CREATED", "APPOINTMENT", appointment.getId());
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

        Instant previousStart = appointment.getStart();
        String previousStatus = appointment.getStatus().name();

        appointment.reschedule(input.start().toInstant());

        validate(appointment, id);
        recordHistory(appointment, "RESCHEDULED", previousStart, previousStatus);
        domainSupport.audit("RESCHEDULED", "APPOINTMENT", id);
        domainSupport.entityManager.flush();

        return toDto(appointment);
    }

    @Transactional
    public AppointmentDto changeStatus(UUID id, StatusInput input) {
        domainSupport.writeLock();

        Appointment appointment = domainSupport.find(Appointment.class, id);
        domainSupport.version(appointment, input.version());
        if (!appointment.isOpen()) {
            throw ApiException.conflict("O estado desta consulta é final e não pode ser alterado.");
        }
        if (input.status() == Appointment.Status.AGENDADO || input.status() == appointment.getStatus()) {
            throw ApiException.conflict("Transição de status inválida.");
        }

        boolean finishingAppointment = input.status() == Appointment.Status.CONCLUIDO
            || input.status() == Appointment.Status.NAO_COMPARECEU;
        if (finishingAppointment && appointment.getEnd().isAfter(Instant.now())) {
            throw ApiException.conflict(
                "Aguarde o horário de término para concluir ou marcar não comparecimento."
            );
        }

        String previousStatus = appointment.getStatus().name();
        appointment.changeStatus(input.status());

        recordHistory(appointment, "STATUS_CHANGED", appointment.getStart(), previousStatus);
        domainSupport.audit("STATUS_CHANGED", "APPOINTMENT", id);
        domainSupport.entityManager.flush();

        return toDto(appointment);
    }

    private void validate(Appointment appointment, UUID ignoredAppointmentId) {
        if (!appointment.getStart().isAfter(Instant.now())) {
            throw ApiException.bad("O agendamento precisa começar no futuro.");
        }

        Patient patient = domainSupport.find(Patient.class, appointment.getPatientId());
        if (!patient.isActive()) {
            throw ApiException.conflict("O paciente está inativo.");
        }

        Professional professional = domainSupport.find(Professional.class, appointment.getProfessionalId());
        if (!professional.isActive()) {
            throw ApiException.conflict("O profissional está inativo.");
        }

        ClinicService service = domainSupport.find(ClinicService.class, appointment.getServiceId());
        if (!service.isActive()) {
            throw ApiException.conflict("O serviço está inativo.");
        }
        if (!professional.offersService(appointment.getServiceId())) {
            throw ApiException.conflict("O profissional não está vinculado a este serviço.");
        }

        availabilityService.validateSlot(
            appointment.getProfessionalId(),
            appointment.getStart(),
            appointment.getEnd()
        );
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
            .setParameter("professional", appointment.getProfessionalId())
            .setParameter("patient", appointment.getPatientId())
            .setParameter("start", appointment.getStart())
            .setParameter("end", appointment.getEnd());
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
        AppointmentHistory history = new AppointmentHistory(
            appointment.getId(),
            DomainSupport.actorId(),
            action,
            previousStart,
            appointment.getStart(),
            previousStatus,
            appointment.getStatus().name()
        );
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
                history.getId(),
                history.getAction(),
                domainSupport.find(AppUser.class, history.getActorId()).getName(),
                DomainSupport.offset(history.getCreatedAt()),
                DomainSupport.offset(history.getPreviousStart()),
                DomainSupport.offset(history.getNewStart()),
                history.getPreviousStatus(),
                history.getNewStatus()
            ))
            .toList();
    }
}
