package com.medisalud.agendamiento.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medisalud.agendamiento.dto.PacienteRequest;
import com.medisalud.agendamiento.dto.PacienteResponse;
import com.medisalud.agendamiento.service.PacienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Pacientes", description = "Registro y consulta de pacientes (RF-02)")
@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @Operation(summary = "Registrar un paciente")
    @PostMapping
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
        PacienteResponse creado = pacienteService.crear(request);
        return ResponseEntity
                .created(URI.create("/api/pacientes/" + creado.id()))
                .body(creado);
    }

    @Operation(summary = "Listar todos los pacientes")
    @GetMapping
    public ResponseEntity<List<PacienteResponse>> listar() {
        return ResponseEntity.ok(pacienteService.listar());
    }

    @Operation(summary = "Obtener un paciente por su id")
    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }
}
