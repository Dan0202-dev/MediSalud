package com.medisalud.agendamiento.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medisalud.agendamiento.domain.Penalizacion;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, Long> {

    /** RN-05: cuenta las penalizaciones de un paciente desde una fecha dada. */
    long countByPacienteIdAndFechaPenalizacionAfter(Long pacienteId, LocalDateTime desde);
}
