package com.medisalud.agendamiento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medisalud.agendamiento.domain.Medico;
import com.medisalud.agendamiento.dto.MedicoRequest;
import com.medisalud.agendamiento.dto.MedicoResponse;
import com.medisalud.agendamiento.repository.MedicoRepository;

@ExtendWith(MockitoExtension.class)
class MedicoServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private MedicoService medicoService;

    @Test
    void crear_soloCamposObligatorios_telefonoYEmailQuedanNull() {
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> {
            Medico m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        // RF-01: telefono y email son opcionales -> se omiten (null)
        MedicoResponse res = medicoService.crear(
                new MedicoRequest("Dra. Ana Lopez", "Dermatologia", null, null));

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.telefono()).isNull();
        assertThat(res.email()).isNull();
    }

    @Test
    void crear_opcionalesEnBlanco_seNormalizanANull() {
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicoResponse res = medicoService.crear(
                new MedicoRequest("Dr. Carlos Ruiz", "Pediatria", "   ", ""));

        assertThat(res.telefono()).isNull();
        assertThat(res.email()).isNull();
    }

    @Test
    void crear_conTelefonoYEmail_losConserva() {
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicoResponse res = medicoService.crear(
                new MedicoRequest("Dra. Maria Gonzalez", "Cardiologia", "555-1001", "maria@medisalud.com"));

        assertThat(res.telefono()).isEqualTo("555-1001");
        assertThat(res.email()).isEqualTo("maria@medisalud.com");
    }
}
