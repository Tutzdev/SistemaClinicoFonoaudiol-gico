package br.com.espacosinapse.professionals;

import br.com.espacosinapse.common.ApiDtos.ActiveInput;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.ProfessionalDto;
import br.com.espacosinapse.common.ApiDtos.ProfessionalInput;
import br.com.espacosinapse.common.ApiDtos.PublicProfessionalDto;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import br.com.espacosinapse.services.ClinicService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProfessionalService {
    private final ProfessionalRepository professionalRepository;
    private final DomainSupport domainSupport;

    public ProfessionalService(
        ProfessionalRepository professionalRepository,
        DomainSupport domainSupport
    ) {
        this.professionalRepository = professionalRepository;
        this.domainSupport = domainSupport;
    }

    public static ProfessionalDto toDto(Professional professional) {
        return new ProfessionalDto(
            professional.id,
            professional.name,
            professional.email,
            professional.phone,
            professional.registration,
            professional.region,
            professional.bio,
            Set.copyOf(professional.serviceIds),
            professional.active,
            professional.published,
            professional.version,
            professional.createdAt,
            professional.updatedAt
        );
    }

    public PageDto<ProfessionalDto> list(String search, int page, int size) {
        String term = search.toLowerCase(Locale.ROOT);

        return PageDto.from(professionalRepository.findAll(
            (root, query, builder) -> builder.like(builder.lower(root.get("name")), "%" + term + "%"),
            DomainSupport.page(page, size, "name")
        ).map(ProfessionalService::toDto));
    }

    public ProfessionalDto get(UUID id) {
        Professional professional = domainSupport.find(Professional.class, id);

        return toDto(professional);
    }

    @Transactional
    public ProfessionalDto create(ProfessionalInput input) {
        domainSupport.writeLock();

        Professional professional = new Professional();
        apply(professional, input);

        domainSupport.entityManager.persist(professional);
        domainSupport.audit("CREATED", "PROFESSIONAL", professional.id);
        domainSupport.entityManager.flush();

        return toDto(professional);
    }

    @Transactional
    public ProfessionalDto update(UUID id, ProfessionalInput input) {
        domainSupport.writeLock();

        Professional professional = domainSupport.find(Professional.class, id);
        domainSupport.version(professional, input.version());

        var conflicts = domainSupport.openFuture("professionalId", id).stream()
            .filter(appointment -> !input.serviceIds().contains(appointment.serviceId))
            .toList();
        if (!conflicts.isEmpty()) {
            throw DomainSupport.conflicts(
                "Resolva as consultas antes de remover o serviço deste profissional.",
                conflicts
            );
        }

        apply(professional, input);
        domainSupport.audit("UPDATED", "PROFESSIONAL", id);
        domainSupport.entityManager.flush();

        return toDto(professional);
    }

    private void apply(Professional professional, ProfessionalInput input) {
        DomainSupport.phone(input.phone(), false);

        String registration = DomainSupport.clean(input.registration());
        String region = DomainSupport.clean(input.region());
        if (input.published() && (registration == null || region == null)) {
            throw ApiException.bad("Para publicar, confirme o registro CRFa e a região do profissional.");
        }

        for (UUID serviceId : input.serviceIds()) {
            domainSupport.find(ClinicService.class, serviceId);
        }

        professional.name = input.name().strip();
        professional.email = DomainSupport.clean(input.email());
        professional.phone = DomainSupport.clean(input.phone());
        professional.registration = registration;
        professional.region = region;
        professional.bio = DomainSupport.clean(input.bio());
        professional.published = input.published();
        professional.serviceIds.clear();
        professional.serviceIds.addAll(input.serviceIds());
    }

    @Transactional
    public ProfessionalDto active(UUID id, ActiveInput input) {
        domainSupport.writeLock();

        Professional professional = domainSupport.find(Professional.class, id);
        domainSupport.version(professional, input.version());
        if (!input.active()) {
            domainSupport.requireNoFuture("professionalId", id);
        }

        professional.active = input.active();
        domainSupport.audit(input.active() ? "ACTIVATED" : "INACTIVATED", "PROFESSIONAL", id);
        domainSupport.entityManager.flush();

        return toDto(professional);
    }

    @Transactional
    public void delete(UUID id) {
        domainSupport.writeLock();

        Professional professional = domainSupport.find(Professional.class, id);
        domainSupport.requireNoHistory("professionalId", id);

        domainSupport.entityManager.remove(professional);
        domainSupport.audit("DELETED", "PROFESSIONAL", id);
        domainSupport.entityManager.flush();
    }

    public List<PublicProfessionalDto> published() {
        return professionalRepository.findAll(
            (root, query, builder) -> builder.and(
                builder.isTrue(root.get("active")),
                builder.isTrue(root.get("published"))
            ),
            Sort.by("name")
        ).stream()
            .map(professional -> new PublicProfessionalDto(
                professional.id,
                professional.name,
                professional.registration,
                professional.region,
                professional.bio
            ))
            .toList();
    }
}
