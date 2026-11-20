package com.reservaslab.reservas.infraestructure.drivers_adapters.external_repository;

import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.gateway.NotificacionGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adaptador que le pide al microservicio Notificaciones que envíe un correo. */
@Component
public class NotificacionGatewayImpl implements NotificacionGateway {

    private final RestClient notificacionesRestClient;

    public NotificacionGatewayImpl(@Qualifier("notificacionesRestClient") RestClient notificacionesRestClient) {
        this.notificacionesRestClient = notificacionesRestClient;
    }

    // Sin destinatario: Notificaciones busca el correo del usuario en el servicio Usuarios
    public record NotificacionRequest(Long idUsuario, String asunto, String mensaje, String tipo) {
    }

    @Override
    public void notificar(Long idUsuario, String asunto, String mensaje) {
        try {
            notificacionesRestClient.post()
                    .uri("/api/notificaciones/enviar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotificacionRequest(idUsuario, asunto, mensaje, "RESERVA"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No fue posible enviar la notificación", e);
        }
    }
}
