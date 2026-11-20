package com.reservaslab.reservas.infraestructure.drivers_adapters.external_repository;

import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.UsuarioInfo;
import com.reservaslab.reservas.domain.model.gateway.UsuarioGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Adaptador que consulta el microservicio Usuarios por REST. */
@Component
public class UsuarioGatewayImpl implements UsuarioGateway {

    private final RestClient usuariosRestClient;

    public UsuarioGatewayImpl(@Qualifier("usuariosRestClient") RestClient usuariosRestClient) {
        this.usuariosRestClient = usuariosRestClient;
    }

    // Usuarios responde más campos (correo, rol); aquí solo se toman estos
    public record UsuarioResponse(Long idUsuario, String nombre, Boolean activo) {
    }

    @Override
    public Optional<UsuarioInfo> consultar(Long idUsuario) {
        try {
            UsuarioResponse respuesta = usuariosRestClient.get()
                    .uri("/api/usuarios/{idUsuario}/validar", idUsuario)
                    .retrieve()
                    .body(UsuarioResponse.class);
            return Optional.ofNullable(respuesta)
                    .map(usuario -> new UsuarioInfo(usuario.idUsuario(), usuario.nombre(),
                            Boolean.TRUE.equals(usuario.activo())));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();                       // El usuario no existe
        } catch (RestClientException e) {
            throw new ServicioExternoException("No fue posible consultar el usuario en el servicio Usuarios", e);
        }
    }
}
