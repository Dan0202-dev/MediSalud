package com.medisalud.agendamiento.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medisalud.agendamiento.domain.Cita;
import com.medisalud.agendamiento.domain.EstadoCita;
import com.medisalud.agendamiento.domain.Medico;
import com.medisalud.agendamiento.domain.Paciente;
import com.medisalud.agendamiento.domain.Penalizacion;
import com.medisalud.agendamiento.dto.CitaRequest;
import com.medisalud.agendamiento.dto.CitaResponse;
import com.medisalud.agendamiento.dto.FranjaDisponibleResponse;
import com.medisalud.agendamiento.exception.BusinessRuleException;
import com.medisalud.agendamiento.exception.ResourceNotFoundException;
import com.medisalud.agendamiento.repository.CitaRepository;
import com.medisalud.agendamiento.repository.CitaSpecifications;
import com.medisalud.agendamiento.repository.PenalizacionRepository;

/**
 * Orquesta el ciclo de vida de las citas y aplica las reglas de negocio
 * RN-02 (no duplicidad), RN-03 (edad), RN-04 (conflicto de paciente),
 * RN-05 (penalizacion por cancelacion tardia) y RN-06 (reprogramacion).
 */
@Service
public class CitaService {

    /** Antelacion minima para cancelar sin penalizacion (RN-05). */
    private static final Duration ANTELACION_SIN_PENALIZACION = Duration.ofHours(2);

    private final CitaRepository citaRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final MedicoService medicoService;
    private final PacienteService pacienteService;
    private final HorarioAtencionService horario;
    private final Clock clock;

    @Value("${app.penalizaciones.umbral:3}")
    private int umbralPenalizaciones;

    @Value("${app.penalizaciones.ventana-dias:30}")
    private int ventanaPenalizacionesDias;

    @Value("${app.disponibilidad.max-dias:31}")
    private int maxDiasDisponibilidad;

    public CitaService(CitaRepository citaRepository, PenalizacionRepository penalizacionRepository,
            MedicoService medicoService, PacienteService pacienteService,
            HorarioAtencionService horario, Clock clock) {
        this.citaRepository = citaRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.medicoService = medicoService;
        this.pacienteService = pacienteService;
        this.horario = horario;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ RF-03
    @Transactional
    public CitaResponse reservar(CitaRequest request) {
        Paciente paciente = pacienteService.buscarEntidad(request.pacienteId());
        Medico medico = medicoService.buscarEntidad(request.medicoId());

        validarEdad(paciente);                       // RN-03
        validarNoBloqueadoPorPenalizaciones(paciente); // RN-05

        Cita cita = crearCitaProgramada(paciente, medico, request.fechaHora());
        return CitaResponse.fromEntity(cita);
    }

    // ------------------------------------------------------------------ RF-05
    @Transactional
    public CitaResponse cancelar(Long citaId) {
        Cita cita = buscarEntidad(citaId);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new BusinessRuleException(
                    "Solo se pueden cancelar citas en estado PROGRAMADA (estado actual: " + cita.getEstado() + ")");
        }
        LocalDateTime ahora = ahora();
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setFechaCancelacion(ahora);
        aplicarPenalizacionSiTardia(cita, ahora); // RN-05
        return CitaResponse.fromEntity(cita);
    }

    /**
     * Marca una cita como ATENDIDA. Solo aplica a citas PROGRAMADAS cuya hora ya
     * transcurrio (no se puede atender una cita futura).
     */
    @Transactional
    public CitaResponse atender(Long citaId) {
        Cita cita = buscarEntidad(citaId);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new BusinessRuleException(
                    "Solo se pueden atender citas en estado PROGRAMADA (estado actual: " + cita.getEstado() + ")");
        }
        if (cita.getFechaHora().isAfter(ahora())) {
            throw new BusinessRuleException("No se puede marcar como ATENDIDA una cita cuya hora aun no ha llegado");
        }
        cita.setEstado(EstadoCita.ATENDIDA);
        return CitaResponse.fromEntity(cita);
    }

    // ------------------------------------------------------------------ RN-06
    @Transactional
    public CitaResponse reprogramar(Long citaId, LocalDateTime nuevaFechaHora) {
        Cita original = buscarEntidad(citaId);
        if (original.getEstado() != EstadoCita.PROGRAMADA) {
            throw new BusinessRuleException(
                    "Solo se pueden reprogramar citas en estado PROGRAMADA (estado actual: "
                            + original.getEstado() + ")");
        }
        Paciente paciente = original.getPaciente();
        Medico medico = original.getMedico();

        // 1. Cancelar la cita anterior (aplicando RN-05 si corresponde).
        LocalDateTime ahora = ahora();
        original.setEstado(EstadoCita.CANCELADA);
        original.setFechaCancelacion(ahora);
        aplicarPenalizacionSiTardia(original, ahora);
        citaRepository.flush(); // la anterior queda CANCELADA antes de validar disponibilidad

        // 2 y 3. Crear la nueva validando RN-01, RN-02 y RN-04 (la reprogramacion
        // no vuelve a aplicar el bloqueo por penalizaciones de RN-05).
        Cita nueva = crearCitaProgramada(paciente, medico, nuevaFechaHora);
        return CitaResponse.fromEntity(nueva);
    }

    // ------------------------------------------------------------------ RF-06
    @Transactional(readOnly = true)
    public List<CitaResponse> listar(Long medicoId, Long pacienteId, EstadoCita estado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        Specification<Cita> spec = Specification.where(null);
        if (medicoId != null) {
            spec = spec.and(CitaSpecifications.conMedico(medicoId));
        }
        if (pacienteId != null) {
            spec = spec.and(CitaSpecifications.conPaciente(pacienteId));
        }
        if (estado != null) {
            spec = spec.and(CitaSpecifications.conEstado(estado));
        }
        if (fechaInicio != null) {
            spec = spec.and(CitaSpecifications.desde(fechaInicio));
        }
        if (fechaFin != null) {
            spec = spec.and(CitaSpecifications.hasta(fechaFin));
        }
        return citaRepository.findAll(spec).stream()
                .map(CitaResponse::fromEntity)
                .toList();
    }

    // ------------------------------------------------------------------ RF-04
    @Transactional(readOnly = true)
    public List<FranjaDisponibleResponse> franjasDisponibles(Long medicoId, LocalDate fechaInicio, LocalDate fechaFin) {
        medicoService.buscarEntidad(medicoId); // 404 si el medico no existe
        if (fechaFin.isBefore(fechaInicio)) {
            throw new BusinessRuleException("fechaFin no puede ser anterior a fechaInicio");
        }
        if (ChronoUnit.DAYS.between(fechaInicio, fechaFin) > maxDiasDisponibilidad) {
            throw new BusinessRuleException("El rango de consulta no puede superar " + maxDiasDisponibilidad + " dias");
        }

        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.plusDays(1).atStartOfDay();
        Set<LocalDateTime> ocupadas = citaRepository
                .findByMedicoIdAndEstadoAndFechaHoraBetween(medicoId, EstadoCita.PROGRAMADA, desde, hasta)
                .stream()
                .map(Cita::getFechaHora)
                .collect(Collectors.toSet());

        List<FranjaDisponibleResponse> disponibles = new ArrayList<>();
        for (LocalDate dia = fechaInicio; !dia.isAfter(fechaFin); dia = dia.plusDays(1)) {
            for (LocalDateTime franja : horario.generarFranjas(dia)) {
                if (!ocupadas.contains(franja)) {
                    disponibles.add(new FranjaDisponibleResponse(franja, franja.plus(HorarioAtencionService.DURACION_FRANJA)));
                }
            }
        }
        return disponibles;
    }

    @Transactional(readOnly = true)
    public CitaResponse obtenerPorId(Long id) {
        return CitaResponse.fromEntity(buscarEntidad(id));
    }

    // --------------------------------------------------------------- internos

    private Cita buscarEntidad(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cita", id));
    }

    /** Crea y persiste una cita PROGRAMADA validando RN-01, RN-02 y RN-04. */
    private Cita crearCitaProgramada(Paciente paciente, Medico medico, LocalDateTime fechaHora) {
        validarFranja(fechaHora);                 // RN-01
        validarMedicoDisponible(medico, fechaHora); // RN-02
        validarPacienteSinConflicto(paciente, fechaHora); // RN-04

        Cita cita = Cita.builder()
                .paciente(paciente)
                .medico(medico)
                .fechaHora(fechaHora)
                .estado(EstadoCita.PROGRAMADA)
                .build();
        return citaRepository.save(cita);
    }

    /** RN-01: la franja debe ser futura y valida dentro del horario de atencion. */
    private void validarFranja(LocalDateTime fechaHora) {
        if (!fechaHora.isAfter(ahora())) {
            throw new BusinessRuleException("La cita debe programarse en una fecha/hora futura");
        }
        if (!horario.esFranjaValida(fechaHora)) {
            throw new BusinessRuleException(
                    "El horario " + fechaHora + " no es una franja valida de atencion "
                            + "(franjas de 30 min; L-V 08:00-18:00, Sab 08:00-13:00; sin domingos ni festivos)");
        }
    }

    /** RN-02: el medico no puede tener otra cita programada en la misma franja. */
    private void validarMedicoDisponible(Medico medico, LocalDateTime fechaHora) {
        if (citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medico.getId(), fechaHora, EstadoCita.PROGRAMADA)) {
            throw new BusinessRuleException("El medico ya tiene una cita programada en esa franja horaria");
        }
    }

    /**
     * RN-04: el paciente no puede tener otra cita programada en la misma franja
     * (no puede estar en dos lugares a la vez, aunque sea con otro medico).
     */
    private void validarPacienteSinConflicto(Paciente paciente, LocalDateTime fechaHora) {
        if (citaRepository.existsByPacienteIdAndFechaHoraAndEstado(paciente.getId(), fechaHora, EstadoCita.PROGRAMADA)) {
            throw new BusinessRuleException("El paciente ya tiene una cita programada en esa franja horaria");
        }
    }

    /** RN-03: no se aceptan pacientes con fecha de nacimiento futura. */
    private void validarEdad(Paciente paciente) {
        LocalDate nacimiento = paciente.getFechaNacimiento();
        if (nacimiento != null && nacimiento.isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("La fecha de nacimiento del paciente es futura; no se puede agendar");
        }
    }

    /** RN-05: bloquea al paciente con demasiadas penalizaciones recientes. */
    private void validarNoBloqueadoPorPenalizaciones(Paciente paciente) {
        LocalDateTime desde = ahora().minusDays(ventanaPenalizacionesDias);
        long penalizaciones = penalizacionRepository
                .countByPacienteIdAndFechaPenalizacionAfter(paciente.getId(), desde);
        if (penalizaciones >= umbralPenalizaciones) {
            throw new BusinessRuleException("El paciente tiene " + penalizaciones
                    + " penalizaciones en los ultimos " + ventanaPenalizacionesDias
                    + " dias y no puede agendar nuevas citas");
        }
    }

    /** RN-05: registra penalizacion si se cancela con menos de 2 horas de antelacion. */
    private void aplicarPenalizacionSiTardia(Cita cita, LocalDateTime ahora) {
        Duration antelacion = Duration.between(ahora, cita.getFechaHora());
        if (antelacion.compareTo(ANTELACION_SIN_PENALIZACION) < 0) {
            penalizacionRepository.save(Penalizacion.builder()
                    .paciente(cita.getPaciente())
                    .cita(cita)
                    .fechaPenalizacion(ahora)
                    .build());
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
