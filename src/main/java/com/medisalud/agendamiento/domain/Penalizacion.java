package com.medisalud.agendamiento.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Penalizacion aplicada a un paciente por cancelacion tardia (RN-05).
 */
@Entity
@Table(name = "penalizaciones", indexes = {
        @Index(name = "idx_penalizacion_paciente_fecha", columnList = "paciente_id, fecha_penalizacion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Penalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** Cita que origino la penalizacion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @Column(name = "fecha_penalizacion", nullable = false)
    private LocalDateTime fechaPenalizacion;
}
