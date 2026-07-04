package com.medisalud.agendamiento.exception;

/**
 * Se lanza cuando un recurso solicitado no existe. Se traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String recurso, Object id) {
        return new ResourceNotFoundException(recurso + " con id " + id + " no encontrado");
    }
}
