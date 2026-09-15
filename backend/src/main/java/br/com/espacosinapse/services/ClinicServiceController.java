package br.com.espacosinapse.services;

import br.com.espacosinapse.common.ApiDtos.ActiveInput;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.ServiceDto;
import br.com.espacosinapse.common.ApiDtos.ServiceInput;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
public class ClinicServiceController {
    private final ClinicServiceService service;

    public ClinicServiceController(ClinicServiceService service) {
        this.service = service;
    }

    @GetMapping
    PageDto<ServiceDto> list(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(search, page, size);
    }

    @GetMapping("/{id}")
    ServiceDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    ServiceDto create(@Valid @RequestBody ServiceInput input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ServiceDto update(@PathVariable UUID id, @Valid @RequestBody ServiceInput input) {
        return service.update(id, input);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    ServiceDto active(@PathVariable UUID id, @Valid @RequestBody ActiveInput input) {
        return service.active(id, input);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
