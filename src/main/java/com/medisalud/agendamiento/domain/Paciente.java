package com.medisalud.agendamiento.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Paciente que reserva citas (RF-02).
 */
@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 50)
    private String documento;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String email;

    /**
     * Fecha de nacimiento opcional. Si es null, se asume edad 0 al agendar (RN-03).
     */
    @Column
    private LocalDate fechaNacimiento;
}
