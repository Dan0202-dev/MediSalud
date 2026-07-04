package com.medisalud.agendamiento.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Datos de entrada para reprogramar una cita (RN-06).
 */
public record ReprogramarRequest(

        @NotNull(message = "La nueva fecha y hora es obligatoria (formato ISO 8601)")
        LocalDateTime nuevaFechaHora) {
}
