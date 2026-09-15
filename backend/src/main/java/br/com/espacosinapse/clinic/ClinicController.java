package br.com.espacosinapse.clinic;

import br.com.espacosinapse.common.ApiDtos.AuditDto;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.PublicClinicDto;
import br.com.espacosinapse.common.ApiDtos.PublicProfessionalDto;
import br.com.espacosinapse.common.ApiDtos.PublicServiceDto;
import br.com.espacosinapse.common.ApiDtos.SettingsDto;
import br.com.espacosinapse.common.ApiDtos.SettingsInput;
import br.com.espacosinapse.common.AuditService;
import br.com.espacosinapse.professionals.ProfessionalService;
import br.com.espacosinapse.services.ClinicServiceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ClinicController {
    private final ClinicSettingsService clinicSettingsService;
    private final ClinicServiceService clinicService;
    private final ProfessionalService professionalService;
    private final AuditService auditService;

    public ClinicController(
        ClinicSettingsService clinicSettingsService,
        ClinicServiceService clinicService,
        ProfessionalService professionalService,
        AuditService auditService
    ) {
        this.clinicSettingsService = clinicSettingsService;
        this.clinicService = clinicService;
        this.professionalService = professionalService;
        this.auditService = auditService;
    }

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/clinic-settings")
    @PreAuthorize("hasRole('ADMIN')")
    SettingsDto settings() {
        return clinicSettingsService.get();
    }

    @PutMapping("/clinic-settings")
    @PreAuthorize("hasRole('ADMIN')")
    SettingsDto update(@Valid @RequestBody SettingsInput input) {
        return clinicSettingsService.update(input);
    }

    @GetMapping("/public/clinic")
    PublicClinicDto publicClinic() {
        return clinicSettingsService.published();
    }

    @GetMapping("/public/services")
    List<PublicServiceDto> publicServices() {
        return clinicService.published();
    }

    @GetMapping("/public/professionals")
    List<PublicProfessionalDto> publicProfessionals() {
        return professionalService.published();
    }

    @GetMapping("/audit-events")
    @PreAuthorize("hasRole('ADMIN')")
    PageDto<AuditDto> audit(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return auditService.list(page, size);
    }
}
