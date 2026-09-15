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
    private final ProfessionalService professionalService;
    private final AvailabilityService availabilityService;

    public ProfessionalController(
        ProfessionalService professionalService,
        AvailabilityService availabilityService
    ) {
        this.professionalService = professionalService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    PageDto<ProfessionalDto> list(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return professionalService.list(search, page, size);
    }

    @GetMapping("/{id}")
    ProfessionalDto get(@PathVariable UUID id) {
        return professionalService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    ProfessionalDto create(@Valid @RequestBody ProfessionalInput input) {
        return professionalService.create(input);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ProfessionalDto update(@PathVariable UUID id, @Valid @RequestBody ProfessionalInput input) {
        return professionalService.update(id, input);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    ProfessionalDto changeActiveStatus(@PathVariable UUID id, @Valid @RequestBody ActiveInput input) {
        return professionalService.changeActiveStatus(id, input);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        professionalService.delete(id);
    }

    @GetMapping("/{id}/availability")
    AvailabilityInput getAvailability(@PathVariable UUID id) {
        return availabilityService.get(id);
    }

    @PutMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    AvailabilityInput updateAvailability(
        @PathVariable UUID id,
        @Valid @RequestBody AvailabilityInput input
    ) {
        return availabilityService.update(id, input);
    }

    @GetMapping("/{id}/blocks")
    List<BlockDto> blocks(@PathVariable UUID id) {
        return availabilityService.blocks(id);
    }

    @PostMapping("/{id}/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    BlockDto createBlock(@PathVariable UUID id, @Valid @RequestBody BlockInput input) {
        return availabilityService.createBlock(id, input);
    }

    @DeleteMapping("/{id}/blocks/{blockId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBlock(@PathVariable UUID id, @PathVariable UUID blockId) {
        availabilityService.deleteBlock(id, blockId);
    }
}
