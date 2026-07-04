package com.medisalud.agendamiento.exception;

/**
 * Se lanza ante un conflicto de unicidad (p. ej. documento repetido). HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
