package br.com.espacosinapse.common;

import br.com.espacosinapse.common.ApiDtos.AuditDto;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditService {
    private final DomainSupport domainSupport;

    public AuditService(DomainSupport domainSupport) {
        this.domainSupport = domainSupport;
    }

    public PageDto<AuditDto> list(int page, int size) {
        DomainSupport.page(page, size, "createdAt");

        List<AuditDto> content = domainSupport.entityManager
            .createQuery("select e from AuditEvent e order by e.createdAt desc", AuditEvent.class)
            .setFirstResult(Math.multiplyExact(page, size))
            .setMaxResults(size)
            .getResultList()
            .stream()
            .map(AuditService::toDto)
            .toList();
        long total = domainSupport.entityManager
            .createQuery("select count(e) from AuditEvent e", Long.class)
            .getSingleResult();
        int totalPages = (int) Math.ceil((double) total / size);

        return new PageDto<>(content, total, totalPages, page, size);
    }

    private static AuditDto toDto(AuditEvent event) {
        return new AuditDto(
            event.id,
            event.actorId,
            event.action,
            event.entityType,
            event.entityId,
            event.createdAt
        );
    }
}
