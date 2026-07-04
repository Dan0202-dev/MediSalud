package com.medisalud.agendamiento.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.medisalud.agendamiento.dto.MedicoResponse;
import com.medisalud.agendamiento.service.MedicoService;

@WebMvcTest(MedicoController.class)
class MedicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicoService medicoService;

    @Test
    void crear_datosValidos_devuelve201() throws Exception {
        when(medicoService.crear(any()))
                .thenReturn(new MedicoResponse(1L, "Dra. Maria Gonzalez", "Cardiologia", "555-1001",
                        "maria.gonzalez@medisalud.com"));

        String body = """
                {
                  "nombreCompleto": "Dra. Maria Gonzalez",
                  "especialidad": "Cardiologia",
                  "telefono": "555-1001",
                  "email": "maria.gonzalez@medisalud.com"
                }
                """;

        mockMvc.perform(post("/api/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.especialidad").value("Cardiologia"));
    }

    @Test
    void crear_nombreInvalido_devuelve400() throws Exception {
        String body = """
                {
                  "nombreCompleto": "ab",
                  "especialidad": "Cardiologia"
                }
                """;

        mockMvc.perform(post("/api/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nombreCompleto").exists());
    }
}
