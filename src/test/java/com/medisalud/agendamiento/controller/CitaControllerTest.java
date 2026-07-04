package com.medisalud.agendamiento.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.medisalud.agendamiento.domain.EstadoCita;
import com.medisalud.agendamiento.dto.CitaResponse;
import com.medisalud.agendamiento.exception.BusinessRuleException;
import com.medisalud.agendamiento.service.CitaService;

@WebMvcTest(CitaController.class)
class CitaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CitaService citaService;

    private CitaResponse citaProgramada() {
        return new CitaResponse(1L, 1L, "Juan Perez", 1L, "Dra. Maria Gonzalez", "Cardiologia",
                LocalDateTime.of(2026, 7, 6, 10, 0), EstadoCita.PROGRAMADA, null);
    }

    @Test
    void reservar_datosValidos_devuelve201() throws Exception {
        when(citaService.reservar(any())).thenReturn(citaProgramada());

        String body = """
                { "pacienteId": 1, "medicoId": 1, "fechaHora": "2026-07-06T10:00:00" }
                """;

        mockMvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.medicoNombre").value("Dra. Maria Gonzalez"));
    }

    @Test
    void reservar_sinFechaHora_devuelve400() throws Exception {
        String body = """
                { "pacienteId": 1, "medicoId": 1 }
                """;

        mockMvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fechaHora").exists());
    }

    @Test
    void reservar_franjaOcupada_devuelve409() throws Exception {
        when(citaService.reservar(any()))
                .thenThrow(new BusinessRuleException("El medico ya tiene una cita programada en esa franja horaria"));

        String body = """
                { "pacienteId": 1, "medicoId": 1, "fechaHora": "2026-07-06T10:00:00" }
                """;

        mockMvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void cancelar_devuelve200ConEstadoCancelada() throws Exception {
        CitaResponse cancelada = new CitaResponse(1L, 1L, "Juan Perez", 1L, "Dra. Maria Gonzalez", "Cardiologia",
                LocalDateTime.of(2026, 7, 6, 10, 0), EstadoCita.CANCELADA, LocalDateTime.of(2026, 7, 6, 9, 30));
        when(citaService.cancelar(eq(1L))).thenReturn(cancelada);

        mockMvc.perform(post("/api/citas/1/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.fechaCancelacion").exists());
    }
}
