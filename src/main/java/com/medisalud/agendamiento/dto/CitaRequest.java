package com.medisalud.agendamiento.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Datos de entrada para reservar una cita (RF-03).
 * La fecha/hora se interpreta en la zona horaria de la clinica (ver README).
 */
public record CitaRequest(

        @NotNull(message = "El pacienteId es obligatorio")
        Long pacienteId,

        @NotNull(message = "El medicoId es obligatorio")
        Long medicoId,

        @NotNull(message = "La fecha y hora es obligatoria (formato ISO 8601)")
        LocalDateTime fechaHora) {
}
