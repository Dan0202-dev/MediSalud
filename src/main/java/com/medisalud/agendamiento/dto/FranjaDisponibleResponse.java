package com.medisalud.agendamiento.dto;

import java.time.LocalDateTime;

/**
 * Franja horaria de 30 minutos disponible para agendar (RF-04).
 */
public record FranjaDisponibleResponse(
        LocalDateTime inicio,
        LocalDateTime fin) {
}
