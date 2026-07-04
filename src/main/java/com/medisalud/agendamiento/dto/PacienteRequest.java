package com.medisalud.agendamiento.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar un paciente (RF-02).
 */
public record PacienteRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El documento es obligatorio")
        @Size(min = 7, max = 50, message = "El documento debe tener al menos 7 caracteres")
        String documento,

        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^[0-9()+\\-\\s]{7,30}$",
                message = "El telefono debe tener al menos 7 digitos")
        String telefono,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150)
        String email,

        // Opcional. Si viene, no puede ser una fecha futura (RN-03).
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento) {
}
