package com.medisalud.agendamiento.exception;

/**
 * Se lanza cuando se viola una regla de negocio (RN-01..RN-06). HTTP 409.
 * Usada intensivamente en la Fase 2 (agendamiento).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
