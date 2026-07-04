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

import com.medisalud.agendamiento.dto.MedicoRequest;
import com.medisalud.agendamiento.dto.MedicoResponse;
import com.medisalud.agendamiento.service.MedicoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Medicos", description = "Registro y consulta de medicos (RF-01)")
@RestController
@RequestMapping("/api/medicos")
public class MedicoController {

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @Operation(summary = "Registrar un medico")
    @PostMapping
    public ResponseEntity<MedicoResponse> crear(@Valid @RequestBody MedicoRequest request) {
        MedicoResponse creado = medicoService.crear(request);
        return ResponseEntity
                .created(URI.create("/api/medicos/" + creado.id()))
                .body(creado);
    }

    @Operation(summary = "Listar todos los medicos")
    @GetMapping
    public ResponseEntity<List<MedicoResponse>> listar() {
        return ResponseEntity.ok(medicoService.listar());
    }

    @Operation(summary = "Obtener un medico por su id")
    @GetMapping("/{id}")
    public ResponseEntity<MedicoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.obtenerPorId(id));
    }
}
