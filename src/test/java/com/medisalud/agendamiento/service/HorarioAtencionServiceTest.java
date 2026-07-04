package com.medisalud.agendamiento.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class HorarioAtencionServiceTest {

    // 2026-07-06 es lunes, 2026-07-11 sabado, 2026-07-12 domingo.
    private final HorarioAtencionService horario = new HorarioAtencionService("2026-07-07");

    @Test
    void diaEntreSemana_genera20Franjas() {
        List<LocalDateTime> franjas = horario.generarFranjas(LocalDate.of(2026, 7, 6));

        assertThat(franjas).hasSize(20);
        assertThat(franjas.get(0)).isEqualTo(LocalDateTime.of(2026, 7, 6, 8, 0));
        assertThat(franjas.get(19)).isEqualTo(LocalDateTime.of(2026, 7, 6, 17, 30));
    }

    @Test
    void sabado_genera10Franjas() {
        List<LocalDateTime> franjas = horario.generarFranjas(LocalDate.of(2026, 7, 11));

        assertThat(franjas).hasSize(10);
        assertThat(franjas.get(0)).isEqualTo(LocalDateTime.of(2026, 7, 11, 8, 0));
        assertThat(franjas.get(9)).isEqualTo(LocalDateTime.of(2026, 7, 11, 12, 30));
    }

    @Test
    void domingo_sinFranjas() {
        assertThat(horario.generarFranjas(LocalDate.of(2026, 7, 12))).isEmpty();
        assertThat(horario.esDiaLaboral(LocalDate.of(2026, 7, 12))).isFalse();
    }

    @Test
    void festivo_sinFranjas() {
        // 2026-07-07 (martes) fue configurado como festivo.
        assertThat(horario.esDiaLaboral(LocalDate.of(2026, 7, 7))).isFalse();
        assertThat(horario.generarFranjas(LocalDate.of(2026, 7, 7))).isEmpty();
    }

    @Test
    void esFranjaValida_reconoceIniciosDeFranja() {
        assertThat(horario.esFranjaValida(LocalDateTime.of(2026, 7, 6, 8, 0))).isTrue();
        assertThat(horario.esFranjaValida(LocalDateTime.of(2026, 7, 6, 17, 30))).isTrue();
    }

    @Test
    void esFranjaValida_rechazaHorariosFueraDeFranja() {
        assertThat(horario.esFranjaValida(LocalDateTime.of(2026, 7, 6, 8, 15))).isFalse();  // no alineada
        assertThat(horario.esFranjaValida(LocalDateTime.of(2026, 7, 6, 7, 30))).isFalse();  // antes de abrir
        assertThat(horario.esFranjaValida(LocalDateTime.of(2026, 7, 6, 18, 0))).isFalse();  // ultima franja acaba a las 18:00
        assertThat(horario.esFranjaValida(LocalDateTime.of(2026, 7, 11, 13, 0))).isFalse(); // sabado ya cerrado
    }
}
