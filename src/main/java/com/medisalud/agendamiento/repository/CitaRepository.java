package com.medisalud.agendamiento.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.medisalud.agendamiento.domain.Cita;
import com.medisalud.agendamiento.domain.EstadoCita;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    /** RN-02: comprueba si el medico ya tiene una cita en esa franja. */
    boolean existsByMedicoIdAndFechaHoraAndEstado(Long medicoId, LocalDateTime fechaHora, EstadoCita estado);

    /** RN-04: comprueba si el paciente ya tiene una cita en esa franja. */
    boolean existsByPacienteIdAndFechaHoraAndEstado(Long pacienteId, LocalDateTime fechaHora, EstadoCita estado);

    /** RF-04: citas de un medico en un rango, para calcular franjas ocupadas. */
    List<Cita> findByMedicoIdAndEstadoAndFechaHoraBetween(
            Long medicoId, EstadoCita estado, LocalDateTime desde, LocalDateTime hasta);
}
