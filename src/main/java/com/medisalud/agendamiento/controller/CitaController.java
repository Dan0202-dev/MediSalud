package com.medisalud.agendamiento.controller;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medisalud.agendamiento.domain.EstadoCita;
import com.medisalud.agendamiento.dto.CitaRequest;
import com.medisalud.agendamiento.dto.CitaResponse;
import com.medisalud.agendamiento.dto.FranjaDisponibleResponse;
import com.medisalud.agendamiento.dto.ReprogramarRequest;
import com.medisalud.agendamiento.service.CitaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Citas", description = "Reserva, cancelacion, reprogramacion, disponibilidad y listado de citas (RF-03..RF-06)")
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @Operation(summary = "Reservar una cita (RF-03)")
    @PostMapping
    public ResponseEntity<CitaResponse> reservar(@Valid @RequestBody CitaRequest request) {
        CitaResponse creada = citaService.reservar(request);
        return ResponseEntity
                .created(URI.create("/api/citas/" + creada.id()))
                .body(creada);
    }

    @Operation(summary = "Consultar franjas disponibles de un medico en un rango de fechas (RF-04)")
    @GetMapping("/disponibilidad")
    public ResponseEntity<List<FranjaDisponibleResponse>> disponibilidad(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(citaService.franjasDisponibles(medicoId, fechaInicio, fechaFin));
    }

    @Operation(summary = "Listar citas con filtros opcionales (RF-06)")
    @GetMapping
    public ResponseEntity<List<CitaResponse>> listar(
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(citaService.listar(medicoId, pacienteId, estado, fechaInicio, fechaFin));
    }

    @Operation(summary = "Obtener una cita por su id")
    @GetMapping("/{id}")
    public ResponseEntity<CitaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtenerPorId(id));
    }

    @Operation(summary = "Cancelar una cita programada (RF-05)")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<CitaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.cancelar(id));
    }

    @Operation(summary = "Reprogramar una cita a una nueva franja (RN-06)")
    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<CitaResponse> reprogramar(@PathVariable Long id,
            @Valid @RequestBody ReprogramarRequest request) {
        return ResponseEntity.ok(citaService.reprogramar(id, request.nuevaFechaHora()));
    }
}
