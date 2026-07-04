package com.medisalud.agendamiento.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Verifica que la validacion de telefono exija al menos 7 DIGITOS reales
 * (RF-01 / RF-02), no solo 7 caracteres.
 */
class TelefonoValidacionTest {

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

    private boolean telefonoMedicoValido(String telefono) {
        Set<ConstraintViolation<MedicoRequest>> v = validator.validateProperty(
                new MedicoRequest("Dr. Test", "Cardiologia", telefono, null), "telefono");
        return v.isEmpty();
    }

    private boolean telefonoPacienteValido(String telefono) {
        Set<ConstraintViolation<PacienteRequest>> v = validator.validateProperty(
                new PacienteRequest("Paciente Test", "1234567", telefono, "a@b.com", null), "telefono");
        return v.isEmpty();
    }

    @Test
    void telefonoConAlMenos7Digitos_esValido() {
        assertThat(telefonoMedicoValido("555-1001")).isTrue();      // 7 digitos + separador
        assertThat(telefonoMedicoValido("1234567")).isTrue();       // 7 digitos exactos
        assertThat(telefonoMedicoValido("(300) 123-4567")).isTrue();// 10 digitos
        assertThat(telefonoPacienteValido("300 555 1212")).isTrue();
    }

    @Test
    void telefonoConMenosDe7Digitos_esInvalido() {
        assertThat(telefonoMedicoValido("12-34-5")).isFalse();   // 7 chars pero solo 5 digitos
        assertThat(telefonoMedicoValido("123456")).isFalse();    // 6 digitos
        assertThat(telefonoPacienteValido("55-12-3")).isFalse(); // 5 digitos
    }

    @Test
    void telefonoOpcionalDelMedico_admiteVacioONull() {
        assertThat(telefonoMedicoValido(null)).isTrue();
        assertThat(telefonoMedicoValido("")).isTrue();
    }
}
