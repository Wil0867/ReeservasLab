package com.reservaslab.notificaciones.domain.Excepciones;

/** Lo que se busca no existe (404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
