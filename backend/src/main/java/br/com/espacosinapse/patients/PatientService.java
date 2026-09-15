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

    public static PatientDto toDto(Patient patient) {
        return new PatientDto(
            patient.id,
            patient.name,
            patient.birthDate,
            patient.phone,
            patient.email,
            patient.guardianName,
            patient.guardianRelationship,
            patient.guardianPhone,
            patient.active,
            patient.version,
            patient.createdAt,
            patient.updatedAt
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

        Patient patient = new Patient();
        apply(patient, input);

        domainSupport.entityManager.persist(patient);
        domainSupport.audit("CREATED", "PATIENT", patient.id);
        domainSupport.entityManager.flush();

        return toDto(patient);
    }

    @Transactional
    public PatientDto update(UUID id, PatientInput input) {
        domainSupport.writeLock();

        Patient patient = domainSupport.find(Patient.class, id);
        domainSupport.version(patient, input.version());
        apply(patient, input);

        domainSupport.audit("UPDATED", "PATIENT", id);
        domainSupport.entityManager.flush();

        return toDto(patient);
    }

    private void apply(Patient patient, PatientInput input) {
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

        patient.name = input.name().strip();
        patient.birthDate = input.birthDate();
        patient.phone = input.phone().strip();
        patient.email = DomainSupport.clean(input.email());
        patient.guardianName = DomainSupport.clean(input.guardianName());
        patient.guardianRelationship = DomainSupport.clean(input.guardianRelationship());
        patient.guardianPhone = DomainSupport.clean(input.guardianPhone());
    }

    @Transactional
    public PatientDto active(UUID id, ActiveInput input) {
        domainSupport.writeLock();

        Patient patient = domainSupport.find(Patient.class, id);
        domainSupport.version(patient, input.version());
        if (!input.active()) {
            domainSupport.requireNoFuture("patientId", id);
        }

        patient.active = input.active();
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
}
