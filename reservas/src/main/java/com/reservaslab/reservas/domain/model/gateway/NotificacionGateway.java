package com.reservaslab.reservas.domain.model.gateway;

/** Lo que Reservas necesita del microservicio Notificaciones. */
public interface NotificacionGateway {
    void notificar(Long idUsuario, String asunto, String mensaje);
}
