package com.medisalud.agendamiento.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medisalud.agendamiento.domain.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByDocumento(String documento);

    Optional<Paciente> findByDocumento(String documento);
}
