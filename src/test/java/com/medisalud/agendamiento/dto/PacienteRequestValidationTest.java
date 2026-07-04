package com.medisalud.agendamiento.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Validacion del contrato de entrada de pacientes (RF-02) y la parte de RN-03
 * que se valida en el registro (fecha de nacimiento no futura).
 */
class PacienteRequestValidationTest {

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

    private Set<String> camposInvalidos(PacienteRequest req) {
        return validator.validate(req).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private PacienteRequest valido() {
        return new PacienteRequest("Juan Perez", "1234567", "5551234", "juan@mail.com",
                LocalDate.of(1990, 5, 20));
    }

    @Test
    void pacienteValido_sinViolaciones() {
        assertThat(camposInvalidos(valido())).isEmpty();
    }

    @Test
    void pacienteSinFechaNacimiento_esValido() {
        // fechaNacimiento es opcional en el registro (RN-03)
        var req = new PacienteRequest("Juan Perez", "1234567", "5551234", "juan@mail.com", null);
        assertThat(camposInvalidos(req)).isEmpty();
    }

    @Test
    void nombreDemasiadoCorto_esInvalido() {
        var req = new PacienteRequest("ab", "1234567", "5551234", "juan@mail.com", null);
        assertThat(camposInvalidos(req)).contains("nombreCompleto");
    }

    @Test
    void documentoDemasiadoCorto_esInvalido() {
        // documento debe tener al menos 7 caracteres (RF-02)
        var req = new PacienteRequest("Juan Perez", "12345", "5551234", "juan@mail.com", null);
        assertThat(camposInvalidos(req)).contains("documento");
    }

    @Test
    void documentoEnBlanco_esInvalido() {
        var req = new PacienteRequest("Juan Perez", "  ", "5551234", "juan@mail.com", null);
        assertThat(camposInvalidos(req)).contains("documento");
    }

    @Test
    void telefonoObligatorioEnBlanco_esInvalido() {
        var req = new PacienteRequest("Juan Perez", "1234567", "", "juan@mail.com", null);
        assertThat(camposInvalidos(req)).contains("telefono");
    }

    @Test
    void telefonoConMenosDe7Digitos_esInvalido() {
        var req = new PacienteRequest("Juan Perez", "1234567", "55-12-3", "juan@mail.com", null);
        assertThat(camposInvalidos(req)).contains("telefono");
    }

    @Test
    void emailObligatorioEnBlanco_esInvalido() {
        var req = new PacienteRequest("Juan Perez", "1234567", "5551234", "", null);
        assertThat(camposInvalidos(req)).contains("email");
    }

    @Test
    void emailConFormatoInvalido_esInvalido() {
        var req = new PacienteRequest("Juan Perez", "1234567", "5551234", "correo-invalido", null);
        assertThat(camposInvalidos(req)).contains("email");
    }

    @Test
    void fechaNacimientoFutura_esInvalida() {
        // RN-03: no se aceptan fechas de nacimiento futuras
        var req = new PacienteRequest("Juan Perez", "1234567", "5551234", "juan@mail.com",
                LocalDate.of(2999, 1, 1));
        assertThat(camposInvalidos(req)).contains("fechaNacimiento");
    }
}
