package com.reservaslab.notificaciones.domain.Excepciones;

/** La operación no es válida en el estado actual (409). */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
