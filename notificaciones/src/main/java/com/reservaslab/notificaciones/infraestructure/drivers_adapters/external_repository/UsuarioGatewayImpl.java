package com.reservaslab.notificaciones.infraestructure.drivers_adapters.external_repository;

import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import com.reservaslab.notificaciones.domain.model.gateway.UsuarioGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Adaptador que le pregunta al microservicio Usuarios el correo de un usuario. */
@Component
public class UsuarioGatewayImpl implements UsuarioGateway {

    private final RestClient usuariosRestClient;

    public UsuarioGatewayImpl(@Qualifier("usuariosRestClient") RestClient usuariosRestClient) {
        this.usuariosRestClient = usuariosRestClient;
    }

    // Usuarios responde más campos; aquí solo interesa el correo
    public record UsuarioResponse(Long idUsuario, String correo) {
    }

    @Override
    public Optional<String> consultarCorreo(Long idUsuario) {
        try {
            UsuarioResponse respuesta = usuariosRestClient.get()
                    .uri("/api/usuarios/{idUsuario}/validar", idUsuario)
                    .retrieve()
                    .body(UsuarioResponse.class);
            return Optional.ofNullable(respuesta).map(UsuarioResponse::correo);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No fue posible consultar el usuario en el servicio Usuarios", e);
        }
    }
}
