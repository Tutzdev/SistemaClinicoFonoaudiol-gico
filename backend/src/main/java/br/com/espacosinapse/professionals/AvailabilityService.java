package br.com.espacosinapse.professionals;

import br.com.espacosinapse.common.ApiDtos.AvailabilityInput;
import br.com.espacosinapse.common.ApiDtos.BlockDto;
import br.com.espacosinapse.common.ApiDtos.BlockInput;
import br.com.espacosinapse.common.ApiDtos.PeriodInput;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {
    private final DomainSupport domainSupport;

    public AvailabilityService(DomainSupport domainSupport) {
        this.domainSupport = domainSupport;
    }

    private List<AvailabilityPeriod> periods(UUID professionalId) {
        return domainSupport.entityManager
            .createQuery(
                "select p from AvailabilityPeriod p "
                    + "where p.professionalId=:id order by p.dayOfWeek,p.startTime",
                AvailabilityPeriod.class
            )
            .setParameter("id", professionalId)
            .getResultList();
    }

    public AvailabilityInput get(UUID professionalId) {
        domainSupport.find(Professional.class, professionalId);

        List<PeriodInput> periods = periods(professionalId).stream()
            .map(period -> new PeriodInput(
                period.getDayOfWeek(),
                period.getStartTime(),
                period.getEndTime()
            ))
            .toList();

        return new AvailabilityInput(periods);
    }

    private static boolean contains(List<PeriodInput> periods, Instant start, Instant end) {
        var localStart = start.atZone(DomainSupport.CLINIC_ZONE);
        var localEnd = end.atZone(DomainSupport.CLINIC_ZONE);

        return localStart.toLocalDate().equals(localEnd.toLocalDate())
            && periods.stream().anyMatch(period ->
                period.dayOfWeek() == localStart.getDayOfWeek().getValue()
                    && !localStart.toLocalTime().isBefore(period.startTime())
                    && !localEnd.toLocalTime().isAfter(period.endTime())
            );
    }

    @Transactional
    public AvailabilityInput update(UUID professionalId, AvailabilityInput input) {
        domainSupport.writeLock();
        domainSupport.find(Professional.class, professionalId);

        List<PeriodInput> sortedPeriods = input.periods().stream()
            .sorted(Comparator.comparingInt(PeriodInput::dayOfWeek).thenComparing(PeriodInput::startTime))
            .toList();
        validatePeriods(sortedPeriods);

        var conflicts = domainSupport.openFuture("professionalId", professionalId).stream()
            .filter(appointment -> !contains(
                sortedPeriods,
                appointment.getStart(),
                appointment.getEnd()
            ))
            .toList();
        if (!conflicts.isEmpty()) {
            throw DomainSupport.conflicts(
                "A nova disponibilidade afeta consultas existentes. Reagende ou cancele antes de continuar.",
                conflicts
            );
        }

        domainSupport.entityManager
            .createQuery("delete from AvailabilityPeriod p where p.professionalId=:id")
            .setParameter("id", professionalId)
            .executeUpdate();
        for (PeriodInput inputPeriod : sortedPeriods) {
            AvailabilityPeriod period = new AvailabilityPeriod(
                professionalId,
                inputPeriod.dayOfWeek(),
                inputPeriod.startTime(),
                inputPeriod.endTime()
            );
            domainSupport.entityManager.persist(period);
        }

        domainSupport.audit("AVAILABILITY_UPDATED", "PROFESSIONAL", professionalId);
        domainSupport.entityManager.flush();

        return get(professionalId);
    }

    private void validatePeriods(List<PeriodInput> periods) {
        PeriodInput previous = null;
        for (PeriodInput period : periods) {
            boolean invalidInterval = !period.startTime().isBefore(period.endTime());
            boolean hasSecondPrecision = period.startTime().getSecond() != 0
                || period.endTime().getSecond() != 0
                || period.startTime().getNano() != 0
                || period.endTime().getNano() != 0;
            if (invalidInterval || hasSecondPrecision) {
                throw ApiException.bad("Informe períodos válidos, com precisão de minutos.");
            }

            boolean overlapsPrevious = previous != null
                && previous.dayOfWeek() == period.dayOfWeek()
                && period.startTime().isBefore(previous.endTime());
            if (overlapsPrevious) {
                throw ApiException.bad("Os períodos de disponibilidade não podem se sobrepor.");
            }

            previous = period;
        }
    }

    public List<BlockDto> blocks(UUID professionalId) {
        domainSupport.find(Professional.class, professionalId);

        return domainSupport.entityManager
            .createQuery(
                "select b from AvailabilityBlock b where b.professionalId=:id order by b.start",
                AvailabilityBlock.class
            )
            .setParameter("id", professionalId)
            .getResultList()
            .stream()
            .map(AvailabilityService::toBlockDto)
            .toList();
    }

    private static BlockDto toBlockDto(AvailabilityBlock block) {
        return new BlockDto(
            block.getId(),
            DomainSupport.offset(block.getStart()),
            DomainSupport.offset(block.getEnd())
        );
    }

    @Transactional
    public BlockDto createBlock(UUID professionalId, BlockInput input) {
        domainSupport.writeLock();
        domainSupport.find(Professional.class, professionalId);

        Instant start = input.start().toInstant();
        Instant end = input.end().toInstant();
        if (!start.isBefore(end) || !end.isAfter(Instant.now())) {
            throw ApiException.bad("O bloqueio precisa ter início anterior ao fim e terminar no futuro.");
        }

        var conflicts = domainSupport.openFuture("professionalId", professionalId).stream()
            .filter(appointment -> appointment.getStart().isBefore(end)
                && appointment.getEnd().isAfter(start))
            .toList();
        if (!conflicts.isEmpty()) {
            throw DomainSupport.conflicts(
                "O bloqueio afeta consultas existentes. Resolva as consultas antes de continuar.",
                conflicts
            );
        }
        if (hasBlock(professionalId, start, end)) {
            throw ApiException.conflict("Este período já possui um bloqueio.");
        }

        AvailabilityBlock block = new AvailabilityBlock(professionalId, start, end);
        domainSupport.entityManager.persist(block);
        domainSupport.audit("BLOCK_CREATED", "PROFESSIONAL", professionalId);
        domainSupport.entityManager.flush();

        return toBlockDto(block);
    }

    private boolean hasBlock(UUID professionalId, Instant start, Instant end) {
        long count = domainSupport.entityManager
            .createQuery(
                "select count(b) from AvailabilityBlock b "
                    + "where b.professionalId=:id and b.start<:end and b.end>:start",
                Long.class
            )
            .setParameter("id", professionalId)
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult();

        return count > 0;
    }

    @Transactional
    public void deleteBlock(UUID professionalId, UUID blockId) {
        domainSupport.writeLock();

        AvailabilityBlock block = domainSupport.find(AvailabilityBlock.class, blockId);
        if (!block.getProfessionalId().equals(professionalId)) {
            throw ApiException.missing();
        }

        domainSupport.entityManager.remove(block);
        domainSupport.audit("BLOCK_DELETED", "PROFESSIONAL", professionalId);
    }

    public void validateSlot(UUID professionalId, Instant start, Instant end) {
        if (!contains(get(professionalId).periods(), start, end)) {
            throw ApiException.conflict("Horário fora da disponibilidade do profissional.");
        }
        if (hasBlock(professionalId, start, end)) {
            throw ApiException.conflict("O profissional tem um bloqueio neste horário.");
        }
    }
}
