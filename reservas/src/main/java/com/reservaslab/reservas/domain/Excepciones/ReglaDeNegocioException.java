package com.reservaslab.reservas.domain.Excepciones;

/** La petición es válida, pero una regla del negocio la rechaza (cruce de horario, festivo, capacidad...). Se traduce a 409 Conflict. */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
