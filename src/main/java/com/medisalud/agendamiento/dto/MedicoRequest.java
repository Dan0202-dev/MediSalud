package com.medisalud.agendamiento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar/actualizar un medico (RF-01).
 * La validacion vive en el DTO, no en la entidad, para desacoplar el contrato
 * de la API del modelo persistido.
 */
public record MedicoRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombreCompleto,

        @NotBlank(message = "La especialidad es obligatoria")
        @Size(max = 100, message = "La especialidad no puede superar 100 caracteres")
        String especialidad,

        // Opcional (RF-01): puede omitirse o venir vacio; si trae contenido, debe
        // tener al menos 7 digitos (se admiten separadores). El vacio se normaliza a null.
        @Pattern(regexp = "^(|[0-9()+\\-\\s]{7,30})$",
                message = "El telefono debe tener al menos 7 digitos")
        String telefono,

        // Opcional (RF-01): si trae contenido, debe ser un email valido.
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150)
        String email) {
}
