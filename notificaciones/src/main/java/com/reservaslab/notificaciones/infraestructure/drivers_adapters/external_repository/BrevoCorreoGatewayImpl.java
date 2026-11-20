package com.reservaslab.notificaciones.infraestructure.drivers_adapters.external_repository;

import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import com.reservaslab.notificaciones.domain.model.gateway.CorreoGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * API EXTERNA 2: Brevo (https://api.brevo.com), correo transaccional por HTTP.
 * Se usa una API y no SMTP porque los servidores gratuitos suelen bloquear los puertos de correo.
 */
@Component
public class BrevoCorreoGatewayImpl implements CorreoGateway {

    private final RestClient brevoRestClient;
    private final String correoRemitente;
    private final String nombreRemitente;

    public BrevoCorreoGatewayImpl(@Qualifier("brevoRestClient") RestClient brevoRestClient,
                                  @Value("${brevo.remitente.correo}") String correoRemitente,
                                  @Value("${brevo.remitente.nombre}") String nombreRemitente) {
        this.brevoRestClient = brevoRestClient;
        this.correoRemitente = correoRemitente;
        this.nombreRemitente = nombreRemitente;
    }

    // Los nombres de los campos son los que exige Brevo (en inglés)
    public record Remitente(String name, String email) {
    }

    public record Destinatario(String email) {
    }

    public record CorreoRequest(Remitente sender, List<Destinatario> to, String subject, String textContent) {
    }

    @Override
    public void enviar(String destinatario, String asunto, String mensaje) {
        try {
            brevoRestClient.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CorreoRequest(new Remitente(nombreRemitente, correoRemitente),
                            List.of(new Destinatario(destinatario)), asunto, mensaje))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioExternoException("La API de correo rechazó el envío: " + e.getMessage(), e);
        }
    }
}
