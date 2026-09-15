package br.com.espacosinapse.common;

import br.com.espacosinapse.patients.Patient;
import br.com.espacosinapse.professionals.AvailabilityPeriod;
import br.com.espacosinapse.professionals.Professional;
import br.com.espacosinapse.services.ClinicService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {
    private static final UUID SERVICE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID PROFESSIONAL_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
    private static final UUID PATIENT_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000003");

    private final DomainSupport domainSupport;

    public DemoData(DomainSupport domainSupport) {
        this.domainSupport = domainSupport;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        domainSupport.writeLock();
        if (domainSupport.entityManager.find(ClinicService.class, SERVICE_ID) != null) {
            return;
        }

        ClinicService service = new ClinicService(
            SERVICE_ID,
            "DEMONSTRAÇÃO · Atendimento fictício",
            "Dado fictício para testar o painel. Não representa um serviço real da clínica.",
            45
        );
        domainSupport.entityManager.persist(service);

        Professional professional = new Professional(
            PROFESSIONAL_ID,
            "DEMONSTRAÇÃO · Profissional fictício",
            Set.of(SERVICE_ID)
        );
        domainSupport.entityManager.persist(professional);

        Patient patient = new Patient(
            PATIENT_ID,
            "DEMONSTRAÇÃO · Paciente fictício",
            LocalDate.of(1990, 1, 1),
            "(61) 00000-0000"
        );
        domainSupport.entityManager.persist(patient);

        for (int day = 1; day <= 5; day++) {
            AvailabilityPeriod period = new AvailabilityPeriod(
                PROFESSIONAL_ID,
                day,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
            );
            domainSupport.entityManager.persist(period);
        }
    }
}
