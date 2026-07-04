package com.medisalud.agendamiento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medisalud.agendamiento.domain.Paciente;
import com.medisalud.agendamiento.dto.PacienteRequest;
import com.medisalud.agendamiento.dto.PacienteResponse;
import com.medisalud.agendamiento.exception.DuplicateResourceException;
import com.medisalud.agendamiento.exception.ResourceNotFoundException;
import com.medisalud.agendamiento.repository.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService pacienteService;

    private PacienteRequest requestValido() {
        return new PacienteRequest(
                "Juan Perez",
                "1234567",
                "555-2000",
                "juan.perez@mail.com",
                LocalDate.of(1990, 1, 1));
    }

    @Test
    void crear_persisteYDevuelveElPaciente() {
        when(pacienteRepository.existsByDocumento("1234567")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PacienteResponse response = pacienteService.crear(requestValido());

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.documento()).isEqualTo("1234567");
        verify(pacienteRepository).save(any(Paciente.class));
    }

    @Test
    void crear_documentoDuplicado_lanzaExcepcion() {
        when(pacienteRepository.existsByDocumento("1234567")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.crear(requestValido()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("1234567");

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_inexistente_lanza404() {
        when(pacienteRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> pacienteService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
