package com.reservaslab.usuarios.domain.Excepciones;

/** Se traduce a 404 Not Found. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
