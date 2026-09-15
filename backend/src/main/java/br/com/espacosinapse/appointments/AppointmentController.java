package br.com.espacosinapse.appointments;

import br.com.espacosinapse.common.ApiDtos.AppointmentDto;
import br.com.espacosinapse.common.ApiDtos.AppointmentInput;
import br.com.espacosinapse.common.ApiDtos.HistoryDto;
import br.com.espacosinapse.common.ApiDtos.PageDto;
import br.com.espacosinapse.common.ApiDtos.RescheduleInput;
import br.com.espacosinapse.common.ApiDtos.StatusInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    PageDto<AppointmentDto> list(
        @RequestParam(required = false) OffsetDateTime from,
        @RequestParam(required = false) OffsetDateTime to,
        @RequestParam(required = false) UUID professionalId,
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) Appointment.Status status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return appointmentService.list(from, to, professionalId, patientId, status, page, size);
    }

    @GetMapping("/{id}")
    AppointmentDto get(@PathVariable UUID id) {
        return appointmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AppointmentDto create(@Valid @RequestBody AppointmentInput input) {
        return appointmentService.create(input);
    }

    @PatchMapping("/{id}/reschedule")
    AppointmentDto reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleInput input) {
        return appointmentService.reschedule(id, input);
    }

    @PatchMapping("/{id}/status")
    AppointmentDto changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusInput input) {
        return appointmentService.changeStatus(id, input);
    }

    @GetMapping("/{id}/history")
    List<HistoryDto> history(@PathVariable UUID id) {
        return appointmentService.history(id);
    }
}
