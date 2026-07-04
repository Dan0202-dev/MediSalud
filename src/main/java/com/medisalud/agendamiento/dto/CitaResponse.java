package com.medisalud.agendamiento.dto;

import java.time.LocalDateTime;

import com.medisalud.agendamiento.domain.Cita;
import com.medisalud.agendamiento.domain.EstadoCita;

/**
 * Representacion de salida de una cita, con datos minimos de paciente y medico.
 */
public record CitaResponse(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        Long medicoId,
        String medicoNombre,
        String especialidad,
        LocalDateTime fechaHora,
        EstadoCita estado,
        LocalDateTime fechaCancelacion) {

    public static CitaResponse fromEntity(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getPaciente().getId(),
                cita.getPaciente().getNombreCompleto(),
                cita.getMedico().getId(),
                cita.getMedico().getNombreCompleto(),
                cita.getMedico().getEspecialidad(),
                cita.getFechaHora(),
                cita.getEstado(),
                cita.getFechaCancelacion());
    }
}
