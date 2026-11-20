package com.reservaslab.notificaciones.domain.Excepciones;

/** Otro servicio (Usuarios o la API de correo) no respondió o rechazó la petición (503). */
public class ServicioExternoException extends RuntimeException {
    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
