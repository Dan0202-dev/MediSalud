package com.medisalud.agendamiento.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medisalud.agendamiento.domain.Paciente;
import com.medisalud.agendamiento.dto.PacienteRequest;
import com.medisalud.agendamiento.dto.PacienteResponse;
import com.medisalud.agendamiento.exception.DuplicateResourceException;
import com.medisalud.agendamiento.exception.ResourceNotFoundException;
import com.medisalud.agendamiento.repository.PacienteRepository;

/**
 * Logica de negocio para el registro y consulta de pacientes (RF-02).
 */
@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    @Transactional
    public PacienteResponse crear(PacienteRequest request) {
        String documento = request.documento().trim();
        if (pacienteRepository.existsByDocumento(documento)) {
            throw new DuplicateResourceException(
                    "Ya existe un paciente con el documento " + documento);
        }
        Paciente paciente = Paciente.builder()
                .nombreCompleto(request.nombreCompleto().trim())
                .documento(documento)
                .telefono(request.telefono())
                .email(request.email())
                .fechaNacimiento(request.fechaNacimiento())
                .build();
        return PacienteResponse.fromEntity(pacienteRepository.save(paciente));
    }

    @Transactional(readOnly = true)
    public List<PacienteResponse> listar() {
        return pacienteRepository.findAll().stream()
                .map(PacienteResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PacienteResponse obtenerPorId(Long id) {
        return PacienteResponse.fromEntity(buscarEntidad(id));
    }

    /**
     * Devuelve la entidad para uso interno de otros servicios (p. ej. citas).
     */
    @Transactional(readOnly = true)
    public Paciente buscarEntidad(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Paciente", id));
    }
}
