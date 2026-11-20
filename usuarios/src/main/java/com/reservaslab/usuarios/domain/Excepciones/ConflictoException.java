package com.reservaslab.usuarios.domain.Excepciones;

/** La operación choca con el estado actual de los datos. Se traduce a 409 Conflict. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
