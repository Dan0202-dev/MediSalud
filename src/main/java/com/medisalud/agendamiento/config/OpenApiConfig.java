package com.medisalud.agendamiento.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Metadatos de la documentacion OpenAPI/Swagger.
 * UI disponible en http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medisaludOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediSalud - API de Agendamiento de Citas Medicas")
                        .description("""
                                API REST para el agendamiento de citas medicas de la clinica MediSalud.
                                Permite registrar medicos y pacientes, reservar y cancelar citas,
                                consultar franjas disponibles y aplicar las reglas de negocio (RN-01..RN-06).
                                """)
                        .version("v1")
                        .contact(new Contact().name("MediSalud").email("soporte@medisalud.com"))
                        .license(new License().name("MIT")));
    }
}
