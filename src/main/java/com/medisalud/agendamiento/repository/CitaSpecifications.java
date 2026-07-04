package com.medisalud.agendamiento.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.medisalud.agendamiento.domain.Cita;
import com.medisalud.agendamiento.domain.EstadoCita;

/**
 * Criterios de filtrado reutilizables para el listado de citas (RF-06).
 * Cada metodo devuelve una Specification aplicable solo cuando el filtro esta presente.
 */
public final class CitaSpecifications {

    private CitaSpecifications() {
    }

    public static Specification<Cita> conMedico(Long medicoId) {
        return (root, query, cb) -> cb.equal(root.get("medico").get("id"), medicoId);
    }

    public static Specification<Cita> conPaciente(Long pacienteId) {
        return (root, query, cb) -> cb.equal(root.get("paciente").get("id"), pacienteId);
    }

    public static Specification<Cita> conEstado(EstadoCita estado) {
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Cita> desde(LocalDateTime fechaInicio) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaHora"), fechaInicio);
    }

    public static Specification<Cita> hasta(LocalDateTime fechaFin) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaHora"), fechaFin);
    }
}
