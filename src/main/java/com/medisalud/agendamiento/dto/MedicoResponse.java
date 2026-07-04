package com.medisalud.agendamiento.dto;

import com.medisalud.agendamiento.domain.Medico;

/**
 * Representacion de salida de un medico. Nunca se expone la entidad directamente.
 */
public record MedicoResponse(
        Long id,
        String nombreCompleto,
        String especialidad,
        String telefono,
        String email) {

    public static MedicoResponse fromEntity(Medico medico) {
        return new MedicoResponse(
                medico.getId(),
                medico.getNombreCompleto(),
                medico.getEspecialidad(),
                medico.getTelefono(),
                medico.getEmail());
    }
}
