package com.medisalud.agendamiento.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Validacion del contrato de entrada de medicos (RF-01): campos obligatorios,
 * formato de email/telefono y opcionalidad.
 */
class MedicoRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    /** Nombres de las propiedades que incumplen alguna restriccion. */
    private Set<String> camposInvalidos(MedicoRequest req) {
        return validator.validate(req).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void medicoCompletoValido_sinViolaciones() {
        var req = new MedicoRequest("Dra. Maria Gonzalez", "Cardiologia", "555-1001", "maria@medisalud.com");
        assertThat(camposInvalidos(req)).isEmpty();
    }

    @Test
    void medicoSoloObligatorios_sinViolaciones() {
        // telefono y email son opcionales (RF-01)
        var req = new MedicoRequest("Dr. Carlos Ruiz", "Pediatria", null, null);
        assertThat(camposInvalidos(req)).isEmpty();
    }

    @Test
    void nombreDemasiadoCorto_esInvalido() {
        var req = new MedicoRequest("ab", "Cardiologia", null, null);
        assertThat(camposInvalidos(req)).contains("nombreCompleto");
    }

    @Test
    void nombreEnBlanco_esInvalido() {
        var req = new MedicoRequest("   ", "Cardiologia", null, null);
        assertThat(camposInvalidos(req)).contains("nombreCompleto");
    }

    @Test
    void especialidadEnBlanco_esInvalida() {
        var req = new MedicoRequest("Dra. Ana Lopez", "", null, null);
        assertThat(camposInvalidos(req)).contains("especialidad");
    }

    @Test
    void emailConFormatoInvalido_esInvalido() {
        var req = new MedicoRequest("Dra. Ana Lopez", "Dermatologia", null, "esto-no-es-un-email");
        assertThat(camposInvalidos(req)).contains("email");
    }

    @Test
    void telefonoConMenosDe7Digitos_esInvalido() {
        var req = new MedicoRequest("Dra. Ana Lopez", "Dermatologia", "12-34-5", null);
        assertThat(camposInvalidos(req)).contains("telefono");
    }
}
