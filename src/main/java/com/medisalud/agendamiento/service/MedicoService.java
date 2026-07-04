package com.medisalud.agendamiento.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medisalud.agendamiento.domain.Medico;
import com.medisalud.agendamiento.dto.MedicoRequest;
import com.medisalud.agendamiento.dto.MedicoResponse;
import com.medisalud.agendamiento.exception.ResourceNotFoundException;
import com.medisalud.agendamiento.repository.MedicoRepository;

/**
 * Logica de negocio para el registro y consulta de medicos (RF-01).
 */
@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    @Transactional
    public MedicoResponse crear(MedicoRequest request) {
        Medico medico = Medico.builder()
                .nombreCompleto(request.nombreCompleto().trim())
                .especialidad(request.especialidad().trim())
                .telefono(normalizarOpcional(request.telefono()))
                .email(normalizarOpcional(request.email()))
                .build();
        return MedicoResponse.fromEntity(medicoRepository.save(medico));
    }

    /** Campos opcionales (RF-01): un valor ausente o en blanco se guarda como null. */
    private static String normalizarOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    @Transactional(readOnly = true)
    public List<MedicoResponse> listar() {
        return medicoRepository.findAll().stream()
                .map(MedicoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicoResponse obtenerPorId(Long id) {
        return MedicoResponse.fromEntity(buscarEntidad(id));
    }

    /**
     * Devuelve la entidad para uso interno de otros servicios (p. ej. citas).
     */
    @Transactional(readOnly = true)
    public Medico buscarEntidad(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Medico", id));
    }
}
