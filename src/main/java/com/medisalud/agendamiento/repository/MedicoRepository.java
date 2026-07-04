package com.medisalud.agendamiento.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medisalud.agendamiento.domain.Medico;

public interface MedicoRepository extends JpaRepository<Medico, Long> {
}
