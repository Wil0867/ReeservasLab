package com.reservaslab.notificaciones.domain.model.gateway;

/** Envío del correo. Lanza ServicioExternoException si el proveedor no lo acepta. */
public interface CorreoGateway {
    void enviar(String destinatario, String asunto, String mensaje);
}
