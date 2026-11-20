package com.reservaslab.usuarios.domain.Excepciones;

/** La cuenta existe pero está desactivada. Se traduce a 403 Forbidden. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
