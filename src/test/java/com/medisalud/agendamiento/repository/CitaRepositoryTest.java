package com.medisalud.agendamiento.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.medisalud.agendamiento.domain.Cita;
import com.medisalud.agendamiento.domain.EstadoCita;
import com.medisalud.agendamiento.domain.Medico;
import com.medisalud.agendamiento.domain.Paciente;

/**
 * Tests de integracion de las consultas de {@link CitaRepository} contra una BD
 * real (H2 embebida). Verifica que las queries derivadas y las Specification
 * funcionan de verdad, no solo su uso desde el servicio con mocks.
 */
@DataJpaTest
class CitaRepositoryTest {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime SLOT = LocalDateTime.of(2026, 7, 6, 10, 0);

    private Medico medico;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        medico = em.persist(Medico.builder()
                .nombreCompleto("Dra. Maria Gonzalez").especialidad("Cardiologia").build());
        paciente = em.persist(Paciente.builder()
                .nombreCompleto("Juan Perez").documento("1234567")
                .telefono("5551234").email("juan@mail.com").build());
        em.flush();
    }

    private Cita persistirCita(LocalDateTime fechaHora, EstadoCita estado) {
        return em.persist(Cita.builder()
                .medico(medico).paciente(paciente).fechaHora(fechaHora).estado(estado).build());
    }

    @Test
    void existsByMedico_soloCuentaLaFranjaYEstadoExactos() {
        persistirCita(SLOT, EstadoCita.PROGRAMADA);
        em.flush();

        assertThat(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(
                medico.getId(), SLOT, EstadoCita.PROGRAMADA)).isTrue();
        // otra franja
        assertThat(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(
                medico.getId(), SLOT.plusMinutes(30), EstadoCita.PROGRAMADA)).isFalse();
        // otro estado
        assertThat(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(
                medico.getId(), SLOT, EstadoCita.CANCELADA)).isFalse();
    }

    @Test
    void existsByPaciente_detectaConflictoEnLaMismaFranja() {
        persistirCita(SLOT, EstadoCita.PROGRAMADA);
        em.flush();

        assertThat(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(
                paciente.getId(), SLOT, EstadoCita.PROGRAMADA)).isTrue();
        assertThat(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(
                paciente.getId(), SLOT.plusDays(1), EstadoCita.PROGRAMADA)).isFalse();
    }

    @Test
    void findByMedicoEstadoYRango_devuelveSoloLasDelRango() {
        persistirCita(SLOT, EstadoCita.PROGRAMADA);                 // dentro
        persistirCita(SLOT.plusDays(2), EstadoCita.PROGRAMADA);     // fuera del rango
        persistirCita(SLOT.plusMinutes(30), EstadoCita.CANCELADA);  // dentro pero CANCELADA
        em.flush();

        LocalDateTime desde = SLOT.toLocalDate().atStartOfDay();
        LocalDateTime hasta = SLOT.toLocalDate().plusDays(1).atStartOfDay();
        List<Cita> encontradas = citaRepository.findByMedicoIdAndEstadoAndFechaHoraBetween(
                medico.getId(), EstadoCita.PROGRAMADA, desde, hasta);

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.get(0).getFechaHora()).isEqualTo(SLOT);
    }

    @Test
    void specification_filtraPorEstado() {
        persistirCita(SLOT, EstadoCita.PROGRAMADA);
        persistirCita(SLOT.plusMinutes(30), EstadoCita.CANCELADA);
        em.flush();

        List<Cita> programadas = citaRepository.findAll(CitaSpecifications.conEstado(EstadoCita.PROGRAMADA));
        assertThat(programadas).hasSize(1);
        assertThat(programadas.get(0).getEstado()).isEqualTo(EstadoCita.PROGRAMADA);

        List<Cita> delMedico = citaRepository.findAll(CitaSpecifications.conMedico(medico.getId()));
        assertThat(delMedico).hasSize(2);
    }
}
