package com.medisalud.agendamiento.dto;

import java.time.LocalDate;

import com.medisalud.agendamiento.domain.Paciente;

/**
 * Representacion de salida de un paciente.
 */
public record PacienteResponse(
        Long id,
        String nombreCompleto,
        String documento,
        String telefono,
        String email,
        LocalDate fechaNacimiento) {

    public static PacienteResponse fromEntity(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNombreCompleto(),
                paciente.getDocumento(),
                paciente.getTelefono(),
                paciente.getEmail(),
                paciente.getFechaNacimiento());
    }
}
