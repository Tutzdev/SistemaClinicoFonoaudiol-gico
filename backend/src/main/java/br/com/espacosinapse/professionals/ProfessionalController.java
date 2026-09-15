package br.com.espacosinapse.professionals;

import br.com.espacosinapse.common.ApiDtos.ActiveInput;
import br.com.espacosinapse.common.ApiDtos.AvailabilityInput;
import br.com.espacosinapse.common.ApiDtos.BlockDto;
import br.com.espacosinapse.common.ApiDtos.BlockInput;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.ProfessionalDto;
import br.com.espacosinapse.common.ApiDtos.ProfessionalInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/professionals")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
public class ProfessionalController {
    private final ProfessionalService service;
    private final AvailabilityService availability;

    public ProfessionalController(ProfessionalService service, AvailabilityService availability) {
        this.service = service;
        this.availability = availability;
    }

    @GetMapping
    PageDto<ProfessionalDto> list(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(search, page, size);
    }

    @GetMapping("/{id}")
    ProfessionalDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    ProfessionalDto create(@Valid @RequestBody ProfessionalInput input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ProfessionalDto update(@PathVariable UUID id, @Valid @RequestBody ProfessionalInput input) {
        return service.update(id, input);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    ProfessionalDto active(@PathVariable UUID id, @Valid @RequestBody ActiveInput input) {
        return service.active(id, input);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}/availability")
    AvailabilityInput availability(@PathVariable UUID id) {
        return availability.get(id);
    }

    @PutMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    AvailabilityInput availability(@PathVariable UUID id, @Valid @RequestBody AvailabilityInput input) {
        return availability.update(id, input);
    }

    @GetMapping("/{id}/blocks")
    List<BlockDto> blocks(@PathVariable UUID id) {
        return availability.blocks(id);
    }

    @PostMapping("/{id}/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    BlockDto block(@PathVariable UUID id, @Valid @RequestBody BlockInput input) {
        return availability.createBlock(id, input);
    }

    @DeleteMapping("/{id}/blocks/{blockId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBlock(@PathVariable UUID id, @PathVariable UUID blockId) {
        availability.deleteBlock(id, blockId);
    }
}
