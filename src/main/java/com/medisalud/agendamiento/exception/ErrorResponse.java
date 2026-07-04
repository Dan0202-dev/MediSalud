package com.medisalud.agendamiento.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo de error uniforme para toda la API.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse withFields(int status, String error, String message, String path,
            Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, fieldErrors);
    }
}
