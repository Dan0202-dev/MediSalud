package com.medisalud.agendamiento.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.medisalud.agendamiento.domain.Medico;
import com.medisalud.agendamiento.repository.MedicoRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Carga inicial de los medicos de ejemplo (RF-01). Solo inserta si la tabla
 * esta vacia, de modo que sea idempotente con la BD H2 en archivo.
 */
@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final MedicoRepository medicoRepository;

    public DataLoader(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    @Override
    public void run(String... args) {
        if (medicoRepository.count() > 0) {
            log.info("Datos ya cargados; se omite la carga inicial de medicos.");
            return;
        }
        List<Medico> medicos = List.of(
                Medico.builder()
                        .nombreCompleto("Dra. Maria Gonzalez")
                        .especialidad("Cardiologia")
                        .telefono("555-1001")
                        .email("maria.gonzalez@medisalud.com")
                        .build(),
                Medico.builder()
                        .nombreCompleto("Dr. Carlos Ruiz")
                        .especialidad("Pediatria")
                        .telefono("555-1002")
                        .email("carlos.ruiz@medisalud.com")
                        .build(),
                Medico.builder()
                        .nombreCompleto("Dra. Ana Lopez")
                        .especialidad("Dermatologia")
                        .telefono("555-1003")
                        .email("ana.lopez@medisalud.com")
                        .build());
        medicoRepository.saveAll(medicos);
        log.info("Carga inicial completada: {} medicos insertados.", medicos.size());
    }
}
