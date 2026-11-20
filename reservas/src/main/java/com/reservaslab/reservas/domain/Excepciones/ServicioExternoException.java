package com.reservaslab.reservas.domain.Excepciones;

/** Otro microservicio o una API externa no responde. Se traduce a 503 Service Unavailable. */
public class ServicioExternoException extends RuntimeException {
    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
