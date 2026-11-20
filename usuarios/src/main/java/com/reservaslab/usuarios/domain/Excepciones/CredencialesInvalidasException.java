package com.reservaslab.usuarios.domain.Excepciones;

/** Correo o clave incorrectos. Se traduce a 401 Unauthorized. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
