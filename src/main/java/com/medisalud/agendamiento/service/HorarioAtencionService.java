package com.medisalud.agendamiento.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encapsula las reglas del horario laboral de la clinica (RN-01) y la generacion
 * de franjas de 30 minutos. Es la unica fuente de verdad sobre cuando se puede atender.
 *
 * <ul>
 *   <li>Lunes a Viernes: 08:00 - 18:00 (franjas 1..20)</li>
 *   <li>Sabados: 08:00 - 13:00 (franjas 1..10)</li>
 *   <li>Domingos y festivos: sin atencion</li>
 * </ul>
 */
@Service
public class HorarioAtencionService {

    public static final LocalTime APERTURA = LocalTime.of(8, 0);
    public static final LocalTime CIERRE_ENTRE_SEMANA = LocalTime.of(18, 0);
    public static final LocalTime CIERRE_SABADO = LocalTime.of(13, 0);
    public static final Duration DURACION_FRANJA = Duration.ofMinutes(30);

    /** Dias festivos sin atencion (RN-01), configurables via app.festivos. */
    private final Set<LocalDate> festivos;

    public HorarioAtencionService(@Value("${app.festivos:}") String festivosCsv) {
        this.festivos = parseFestivos(festivosCsv);
    }

    /** Un dia es laboral si no es domingo ni festivo. */
    public boolean esDiaLaboral(LocalDate fecha) {
        return fecha.getDayOfWeek() != DayOfWeek.SUNDAY && !festivos.contains(fecha);
    }

    /**
     * Genera todas las franjas de 30 minutos disponibles en un dia segun el horario.
     * Devuelve lista vacia si el dia no es laboral.
     */
    public List<LocalDateTime> generarFranjas(LocalDate fecha) {
        if (!esDiaLaboral(fecha)) {
            return List.of();
        }
        LocalDateTime cierre = fecha.atTime(cierreDe(fecha));
        List<LocalDateTime> franjas = new ArrayList<>();
        LocalDateTime franja = fecha.atTime(APERTURA);
        while (!franja.plus(DURACION_FRANJA).isAfter(cierre)) {
            franjas.add(franja);
            franja = franja.plus(DURACION_FRANJA);
        }
        return franjas;
    }

    /**
     * Indica si una fecha/hora corresponde exactamente al inicio de una franja
     * valida dentro del horario de atencion (RN-01).
     */
    public boolean esFranjaValida(LocalDateTime fechaHora) {
        return generarFranjas(fechaHora.toLocalDate()).contains(fechaHora);
    }

    private LocalTime cierreDe(LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.SATURDAY ? CIERRE_SABADO : CIERRE_ENTRE_SEMANA;
    }

    private static Set<LocalDate> parseFestivos(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
    }
}
