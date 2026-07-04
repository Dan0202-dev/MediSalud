package com.medisalud.agendamiento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.medisalud.agendamiento.domain.Cita;
import com.medisalud.agendamiento.domain.EstadoCita;
import com.medisalud.agendamiento.domain.Medico;
import com.medisalud.agendamiento.domain.Paciente;
import com.medisalud.agendamiento.domain.Penalizacion;
import com.medisalud.agendamiento.dto.CitaRequest;
import com.medisalud.agendamiento.dto.CitaResponse;
import com.medisalud.agendamiento.dto.FranjaDisponibleResponse;
import com.medisalud.agendamiento.exception.BusinessRuleException;
import com.medisalud.agendamiento.repository.CitaRepository;
import com.medisalud.agendamiento.repository.PenalizacionRepository;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    // "Ahora" fijo: lunes 2026-07-06 a las 09:00.
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 9, 0);

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private PenalizacionRepository penalizacionRepository;
    @Mock
    private MedicoService medicoService;
    @Mock
    private PacienteService pacienteService;

    private CitaService citaService;

    private final Paciente paciente = Paciente.builder()
            .id(1L).nombreCompleto("Juan Perez").documento("1234567")
            .telefono("5551234").email("juan@mail.com").build();
    private final Medico medico = Medico.builder()
            .id(1L).nombreCompleto("Dra. Maria Gonzalez").especialidad("Cardiologia").build();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        HorarioAtencionService horario = new HorarioAtencionService("");
        citaService = new CitaService(citaRepository, penalizacionRepository,
                medicoService, pacienteService, horario, clock);
        ReflectionTestUtils.setField(citaService, "umbralPenalizaciones", 3);
        ReflectionTestUtils.setField(citaService, "ventanaPenalizacionesDias", 30);
        ReflectionTestUtils.setField(citaService, "maxDiasDisponibilidad", 31);
    }

    private CitaRequest request(LocalDateTime fechaHora) {
        return new CitaRequest(1L, 1L, fechaHora);
    }

    private void stubPacienteYMedico() {
        when(pacienteService.buscarEntidad(1L)).thenReturn(paciente);
        when(medicoService.buscarEntidad(1L)).thenReturn(medico);
    }

    // ------------------------------------------------------------- RF-03 feliz
    @Test
    void reservar_franjaValida_creaCitaProgramada() {
        stubPacienteYMedico();
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(anyLong(), any())).thenReturn(0L);
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(any(), any(), any())).thenReturn(false);
        when(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(any(), any(), any())).thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CitaResponse res = citaService.reservar(request(LocalDateTime.of(2026, 7, 6, 10, 0)));

        assertThat(res.id()).isEqualTo(100L);
        assertThat(res.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(res.medicoNombre()).isEqualTo("Dra. Maria Gonzalez");
    }

    // ----------------------------------------------------------------- RN-01
    @Test
    void reservar_franjaNoAlineada_rechaza() {
        stubPacienteYMedico();
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(anyLong(), any())).thenReturn(0L);

        assertThatThrownBy(() -> citaService.reservar(request(LocalDateTime.of(2026, 7, 6, 10, 15))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("franja valida");
        verify(citaRepository, never()).save(any());
    }

    @Test
    void reservar_domingo_rechaza() {
        stubPacienteYMedico();
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(anyLong(), any())).thenReturn(0L);

        assertThatThrownBy(() -> citaService.reservar(request(LocalDateTime.of(2026, 7, 12, 10, 0))))
                .isInstanceOf(BusinessRuleException.class);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void reservar_fechaPasada_rechaza() {
        stubPacienteYMedico();
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(anyLong(), any())).thenReturn(0L);

        assertThatThrownBy(() -> citaService.reservar(request(LocalDateTime.of(2026, 7, 6, 8, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("futura");
    }

    // ----------------------------------------------------------------- RN-02
    @Test
    void reservar_medicoOcupado_rechaza() {
        stubPacienteYMedico();
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(anyLong(), any())).thenReturn(0L);
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> citaService.reservar(request(LocalDateTime.of(2026, 7, 6, 10, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("medico ya tiene");
        verify(citaRepository, never()).save(any());
    }

    // ----------------------------------------------------------------- RN-04
    @Test
    void reservar_pacienteConOtraCitaMismaFranja_rechaza() {
        stubPacienteYMedico();
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(anyLong(), any())).thenReturn(0L);
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(any(), any(), any())).thenReturn(false);
        when(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> citaService.reservar(request(LocalDateTime.of(2026, 7, 6, 10, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("paciente ya tiene");
        verify(citaRepository, never()).save(any());
    }

    // ----------------------------------------------------------------- RN-05
    @Test
    void reservar_pacienteConTresPenalizaciones_rechaza() {
        stubPacienteYMedico();
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(anyLong(), any())).thenReturn(3L);

        assertThatThrownBy(() -> citaService.reservar(request(LocalDateTime.of(2026, 7, 6, 10, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("penalizaciones");
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelar_conMenosDe2Horas_registraPenalizacion() {
        Cita cita = citaProgramada(10L, LocalDateTime.of(2026, 7, 6, 10, 0)); // a 1 hora
        when(citaRepository.findById(10L)).thenReturn(Optional.of(cita));

        CitaResponse res = citaService.cancelar(10L);

        assertThat(res.estado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(res.fechaCancelacion()).isEqualTo(AHORA);
        verify(penalizacionRepository).save(any(Penalizacion.class));
    }

    @Test
    void cancelar_conMasDe2Horas_noPenaliza() {
        Cita cita = citaProgramada(11L, LocalDateTime.of(2026, 7, 6, 14, 0)); // a 5 horas
        when(citaRepository.findById(11L)).thenReturn(Optional.of(cita));

        CitaResponse res = citaService.cancelar(11L);

        assertThat(res.estado()).isEqualTo(EstadoCita.CANCELADA);
        verify(penalizacionRepository, never()).save(any());
    }

    @Test
    void cancelar_citaNoProgramada_rechaza() {
        Cita cita = citaProgramada(12L, LocalDateTime.of(2026, 7, 6, 14, 0));
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaRepository.findById(12L)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.cancelar(12L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PROGRAMADA");
    }

    // ----------------------------------------------------------------- RF-04
    @Test
    void franjasDisponibles_excluyeLasOcupadas() {
        LocalDate dia = LocalDate.of(2026, 7, 6); // lunes: 20 franjas
        LocalDateTime ocupada = LocalDateTime.of(2026, 7, 6, 10, 0);
        when(medicoService.buscarEntidad(1L)).thenReturn(medico);
        when(citaRepository.findByMedicoIdAndEstadoAndFechaHoraBetween(
                eq(1L), eq(EstadoCita.PROGRAMADA), any(), any()))
                .thenReturn(List.of(citaProgramada(1L, ocupada)));

        List<FranjaDisponibleResponse> franjas = citaService.franjasDisponibles(1L, dia, dia);

        assertThat(franjas).hasSize(19);
        assertThat(franjas).noneMatch(f -> f.inicio().equals(ocupada));
    }

    @Test
    void franjasDisponibles_rangoInvertido_rechaza() {
        when(medicoService.buscarEntidad(1L)).thenReturn(medico);

        assertThatThrownBy(() -> citaService.franjasDisponibles(
                1L, LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 6)))
                .isInstanceOf(BusinessRuleException.class);
    }

    private Cita citaProgramada(Long id, LocalDateTime fechaHora) {
        return Cita.builder()
                .id(id).paciente(paciente).medico(medico)
                .fechaHora(fechaHora).estado(EstadoCita.PROGRAMADA)
                .build();
    }
}
