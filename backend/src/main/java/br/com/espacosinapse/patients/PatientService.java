package br.com.espacosinapse.patients;

import br.com.espacosinapse.common.ApiDtos.ActiveInput;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.PatientDto;
import br.com.espacosinapse.common.ApiDtos.PatientInput;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patientRepository;
    private final DomainSupport domainSupport;

    public PatientService(PatientRepository patientRepository, DomainSupport domainSupport) {
        this.patientRepository = patientRepository;
        this.domainSupport = domainSupport;
    }

    private static PatientDto toDto(Patient patient) {
        return new PatientDto(
            patient.getId(),
            patient.getName(),
            patient.getBirthDate(),
            patient.getPhone(),
            patient.getEmail(),
            patient.getGuardianName(),
            patient.getGuardianRelationship(),
            patient.getGuardianPhone(),
            patient.isActive(),
            patient.getVersion(),
            patient.getCreatedAt(),
            patient.getUpdatedAt()
        );
    }

    public PageDto<PatientDto> list(String search, int page, int size) {
        String term = search.strip().toLowerCase(Locale.ROOT);

        return PageDto.from(patientRepository.findAll(
            (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), "%" + term + "%"),
                builder.like(root.get("phone"), "%" + term + "%")
            ),
            DomainSupport.page(page, size, "name")
        ).map(PatientService::toDto));
    }

    public PatientDto get(UUID id) {
        Patient patient = domainSupport.find(Patient.class, id);

        return toDto(patient);
    }

    @Transactional
    public PatientDto create(PatientInput input) {
        domainSupport.writeLock();

        PatientDetails details = validateAndNormalize(input);
        Patient patient = new Patient(
            details.name(),
            details.birthDate(),
            details.phone(),
            details.email(),
            details.guardianName(),
            details.guardianRelationship(),
            details.guardianPhone()
        );

        domainSupport.entityManager.persist(patient);
        domainSupport.audit("CREATED", "PATIENT", patient.getId());
        domainSupport.entityManager.flush();

        return toDto(patient);
    }

    @Transactional
    public PatientDto update(UUID id, PatientInput input) {
        domainSupport.writeLock();

        Patient patient = domainSupport.find(Patient.class, id);
        domainSupport.version(patient, input.version());
        PatientDetails details = validateAndNormalize(input);
        patient.updateDetails(
            details.name(),
            details.birthDate(),
            details.phone(),
            details.email(),
            details.guardianName(),
            details.guardianRelationship(),
            details.guardianPhone()
        );

        domainSupport.audit("UPDATED", "PATIENT", id);
        domainSupport.entityManager.flush();

        return toDto(patient);
    }

    private PatientDetails validateAndNormalize(PatientInput input) {
        LocalDate today = LocalDate.now(DomainSupport.CLINIC_ZONE);
        if (input.birthDate().isAfter(today)) {
            throw ApiException.bad("A data de nascimento não pode estar no futuro.");
        }

        DomainSupport.phone(input.phone(), true);
        DomainSupport.phone(input.guardianPhone(), false);

        boolean minor = input.birthDate().isAfter(today.minusYears(18));
        boolean missingGuardian = DomainSupport.clean(input.guardianName()) == null
            || DomainSupport.clean(input.guardianRelationship()) == null
            || DomainSupport.clean(input.guardianPhone()) == null;
        if (minor && missingGuardian) {
            throw ApiException.bad("Pacientes menores de 18 anos precisam de nome, vínculo e telefone do responsável.");
        }

        return new PatientDetails(
            input.name().strip(),
            input.birthDate(),
            input.phone().strip(),
            DomainSupport.clean(input.email()),
            DomainSupport.clean(input.guardianName()),
            DomainSupport.clean(input.guardianRelationship()),
            DomainSupport.clean(input.guardianPhone())
        );
    }

    @Transactional
    public PatientDto changeActiveStatus(UUID id, ActiveInput input) {
        domainSupport.writeLock();

        Patient patient = domainSupport.find(Patient.class, id);
        domainSupport.version(patient, input.version());
        if (!input.active()) {
            domainSupport.requireNoFuture("patientId", id);
        }

        patient.changeActiveStatus(input.active());
        domainSupport.audit(input.active() ? "ACTIVATED" : "INACTIVATED", "PATIENT", id);
        domainSupport.entityManager.flush();

        return toDto(patient);
    }

    @Transactional
    public void delete(UUID id) {
        domainSupport.writeLock();

        Patient patient = domainSupport.find(Patient.class, id);
        domainSupport.requireNoHistory("patientId", id);

        domainSupport.entityManager.remove(patient);
        domainSupport.audit("DELETED", "PATIENT", id);
        domainSupport.entityManager.flush();
    }

    private record PatientDetails(
        String name,
        LocalDate birthDate,
        String phone,
        String email,
        String guardianName,
        String guardianRelationship,
        String guardianPhone
    ) {
    }
}
