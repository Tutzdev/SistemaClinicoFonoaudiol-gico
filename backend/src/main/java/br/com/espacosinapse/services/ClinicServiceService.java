package br.com.espacosinapse.services;

import br.com.espacosinapse.common.ApiDtos.ActiveInput;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.PublicServiceDto;
import br.com.espacosinapse.common.ApiDtos.ServiceDto;
import br.com.espacosinapse.common.ApiDtos.ServiceInput;
import br.com.espacosinapse.common.DomainSupport;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClinicServiceService {
    private final ClinicServiceRepository serviceRepository;
    private final DomainSupport domainSupport;

    public ClinicServiceService(ClinicServiceRepository serviceRepository, DomainSupport domainSupport) {
        this.serviceRepository = serviceRepository;
        this.domainSupport = domainSupport;
    }

    private static ServiceDto toDto(ClinicService service) {
        return new ServiceDto(
            service.getId(),
            service.getName(),
            service.getDescription(),
            service.getDurationMinutes(),
            service.isActive(),
            service.isPublished(),
            service.getVersion(),
            service.getCreatedAt(),
            service.getUpdatedAt()
        );
    }

    public PageDto<ServiceDto> list(String search, int page, int size) {
        String term = search.toLowerCase(Locale.ROOT);

        return PageDto.from(serviceRepository.findAll(
            (root, query, builder) -> builder.like(builder.lower(root.get("name")), "%" + term + "%"),
            DomainSupport.page(page, size, "name")
        ).map(ClinicServiceService::toDto));
    }

    public ServiceDto get(UUID id) {
        ClinicService service = domainSupport.find(ClinicService.class, id);

        return toDto(service);
    }

    @Transactional
    public ServiceDto create(ServiceInput input) {
        domainSupport.writeLock();

        ClinicService service = new ClinicService(
            input.name().strip(),
            DomainSupport.clean(input.description()),
            input.durationMinutes(),
            input.published()
        );

        domainSupport.entityManager.persist(service);
        domainSupport.audit("CREATED", "SERVICE", service.getId());
        domainSupport.entityManager.flush();

        return toDto(service);
    }

    @Transactional
    public ServiceDto update(UUID id, ServiceInput input) {
        domainSupport.writeLock();

        ClinicService service = domainSupport.find(ClinicService.class, id);
        domainSupport.version(service, input.version());
        service.updateDetails(
            input.name().strip(),
            DomainSupport.clean(input.description()),
            input.durationMinutes(),
            input.published()
        );

        domainSupport.audit("UPDATED", "SERVICE", id);
        domainSupport.entityManager.flush();

        return toDto(service);
    }

    @Transactional
    public ServiceDto changeActiveStatus(UUID id, ActiveInput input) {
        domainSupport.writeLock();

        ClinicService service = domainSupport.find(ClinicService.class, id);
        domainSupport.version(service, input.version());
        if (!input.active()) {
            domainSupport.requireNoFuture("serviceId", id);
        }

        service.changeActiveStatus(input.active());
        domainSupport.audit(input.active() ? "ACTIVATED" : "INACTIVATED", "SERVICE", id);
        domainSupport.entityManager.flush();

        return toDto(service);
    }

    @Transactional
    public void delete(UUID id) {
        domainSupport.writeLock();

        ClinicService service = domainSupport.find(ClinicService.class, id);
        domainSupport.requireNoHistory("serviceId", id);

        domainSupport.entityManager.remove(service);
        domainSupport.audit("DELETED", "SERVICE", id);
        domainSupport.entityManager.flush();
    }

    public List<PublicServiceDto> published() {
        return serviceRepository.findAll(
            (root, query, builder) -> builder.and(
                builder.isTrue(root.get("active")),
                builder.isTrue(root.get("published"))
            ),
            Sort.by("name")
        ).stream()
            .map(service -> new PublicServiceDto(
                service.getId(),
                service.getName(),
                service.getDescription()
            ))
            .toList();
    }
}
