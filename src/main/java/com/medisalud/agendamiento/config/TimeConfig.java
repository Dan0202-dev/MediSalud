package com.medisalud.agendamiento.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone un {@link Clock} con la zona horaria de la clinica. Se inyecta en los
 * servicios en lugar de usar LocalDateTime.now() directo, para que las reglas de
 * negocio dependientes del tiempo (RN-05) sean deterministas y testeables.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
