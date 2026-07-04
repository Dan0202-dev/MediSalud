package com.medisalud.agendamiento.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.medisalud.agendamiento.domain.Paciente;
import com.medisalud.agendamiento.domain.Penalizacion;

/**
 * Test de integracion de {@link PenalizacionRepository} (RN-05): el conteo por
 * ventana temporal se ejecuta contra una BD real.
 */
@DataJpaTest
class PenalizacionRepositoryTest {

    @Autowired
    private PenalizacionRepository penalizacionRepository;

    @Autowired
    private TestEntityManager em;

    // "Ahora" de referencia para la ventana de 30 dias.
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 9, 0);

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        paciente = em.persist(Paciente.builder()
                .nombreCompleto("Juan Perez").documento("1234567")
                .telefono("5551234").email("juan@mail.com").build());
        em.flush();
    }

    private void persistirPenalizacion(LocalDateTime fecha) {
        em.persist(Penalizacion.builder().paciente(paciente).fechaPenalizacion(fecha).build());
    }

    @Test
    void cuentaSoloLasPenalizacionesDentroDeLaVentana() {
        persistirPenalizacion(AHORA.minusDays(5));   // dentro de 30 dias
        persistirPenalizacion(AHORA.minusDays(20));  // dentro de 30 dias
        persistirPenalizacion(AHORA.minusDays(40));  // fuera de 30 dias
        em.flush();

        long recientes = penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(
                paciente.getId(), AHORA.minusDays(30));

        assertThat(recientes).isEqualTo(2);
    }

    @Test
    void pacienteSinPenalizaciones_cuentaCero() {
        long recientes = penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(
                paciente.getId(), AHORA.minusDays(30));

        assertThat(recientes).isZero();
    }
}
