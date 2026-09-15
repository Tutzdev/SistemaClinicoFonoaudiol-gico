package br.com.espacosinapse.common;

import br.com.espacosinapse.appointments.Appointment.Status;
import br.com.espacosinapse.users.AppUser.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record PageDto<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
        public static <T> PageDto<T> from(Page<T> page) {
            return new PageDto<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
            );
        }
    }

    public record ActiveInput(@NotNull Boolean active, @NotNull @PositiveOrZero Long version) {}

    public record PatientInput(
        @NotBlank @Size(max = 160) String name,
        @NotNull @PastOrPresent LocalDate birthDate,
        @NotBlank @Size(max = 32) String phone,
        @Email @Size(max = 254) String email,
        @Size(max = 160) String guardianName,
        @Size(max = 80) String guardianRelationship,
        @Size(max = 32) String guardianPhone,
        @PositiveOrZero Long version
    ) {}

    public record PatientDto(
        UUID id,
        String name,
        LocalDate birthDate,
        String phone,
        String email,
        String guardianName,
        String guardianRelationship,
        String guardianPhone,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record ServiceInput(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String description,
        @NotNull @Min(1) @Max(480) Integer durationMinutes,
        boolean published,
        @PositiveOrZero Long version
    ) {}

    public record ServiceDto(
        UUID id,
        String name,
        String description,
        int durationMinutes,
        boolean active,
        boolean published,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record ProfessionalInput(
        @NotBlank @Size(max = 160) String name,
        @Email @Size(max = 254) String email,
        @Size(max = 32) String phone,
        @Size(max = 80) String registration,
        @Size(max = 80) String region,
        @Size(max = 4000) String bio,
        @NotNull @Size(max = 100) Set<@NotNull UUID> serviceIds,
        boolean published,
        @PositiveOrZero Long version
    ) {}

    public record ProfessionalDto(
        UUID id,
        String name,
        String email,
        String phone,
        String registration,
        String region,
        String bio,
        Set<UUID> serviceIds,
        boolean active,
        boolean published,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record PeriodInput(
        @Min(1) @Max(7) int dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
    ) {}

    public record AvailabilityInput(
        @NotNull @Size(max = 35) List<@NotNull @Valid PeriodInput> periods
    ) {}

    public record BlockInput(@NotNull OffsetDateTime start, @NotNull OffsetDateTime end) {}

    public record BlockDto(UUID id, OffsetDateTime start, OffsetDateTime end) {}

    public record AppointmentInput(
        @NotNull UUID patientId,
        @NotNull UUID professionalId,
        @NotNull UUID serviceId,
        @NotNull OffsetDateTime start
    ) {}

    public record RescheduleInput(@NotNull OffsetDateTime start, @NotNull @PositiveOrZero Long version) {}

    public record StatusInput(@NotNull Status status, @NotNull @PositiveOrZero Long version) {}

    public record AppointmentDto(
        UUID id,
        UUID patientId,
        String patientName,
        UUID professionalId,
        String professionalName,
        UUID serviceId,
        String serviceName,
        OffsetDateTime start,
        OffsetDateTime end,
        int durationMinutes,
        Status status,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record HistoryDto(
        UUID id,
        String action,
        String actorName,
        OffsetDateTime createdAt,
        OffsetDateTime previousStart,
        OffsetDateTime newStart,
        String previousStatus,
        String newStatus
    ) {}

    public record UserInput(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull Role role,
        @Size(min = 12, max = 72) String password,
        @PositiveOrZero Long version
    ) {}

    public record UserDto(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record AuthDto(UUID id, String name, String email, Role role) {}

    public record LoginInput(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password
    ) {}

    public record PasswordInput(
        @NotBlank @Size(max = 72) String currentPassword,
        @NotBlank @Size(min = 12, max = 72) String newPassword
    ) {}

    public record SettingsInput(
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Size(max = 4000) String description,
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 32) String whatsapp,
        @Size(max = 500) String publicAddress,
        boolean addressConfirmed,
        @NotNull @PositiveOrZero Long version
    ) {}

    public record SettingsDto(
        String displayName,
        String description,
        String phone,
        String email,
        String whatsapp,
        String publicAddress,
        boolean addressConfirmed,
        long version
    ) {}

    public record PublicClinicDto(
        String displayName,
        String description,
        String phone,
        String email,
        String whatsapp,
        String publicAddress,
        boolean addressConfirmed,
        String legalName,
        String cnpj
    ) {}

    public record PublicServiceDto(UUID id, String name, String description) {}

    public record PublicProfessionalDto(UUID id, String name, String registration, String region, String bio) {}

    public record AuditDto(UUID id, UUID actorId, String action, String entityType, UUID entityId, Instant createdAt) {}
}
