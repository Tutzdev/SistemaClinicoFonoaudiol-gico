package br.com.espacosinapse.common;

import java.util.List;
import java.util.UUID;

public class ApiException extends RuntimeException {
    public final int status;
    public final List<UUID> conflictingAppointmentIds;

    public ApiException(int status, String message) {
        this(status, message, List.of());
    }

    public ApiException(int status, String message, List<UUID> ids) {
        super(message);
        this.status = status;
        this.conflictingAppointmentIds = List.copyOf(ids);
    }

    public static ApiException bad(String message) {
        return new ApiException(400, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }

    public static ApiException missing() {
        return new ApiException(404, "Registro não encontrado.");
    }
}
