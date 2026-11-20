package com.reservaslab.reservas.infraestructure.drivers_adapters.external_repository;

import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.UsuarioInfo;
import com.reservaslab.reservas.domain.model.gateway.UsuarioGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador que consulta el microservicio Usuarios por REST.
 * Guarda cada usuario encontrado durante 30 s: así crear varias reservas seguidas no llama
 * a Usuarios cada vez. Costo: un usuario recién desactivado puede reservar hasta 30 s más.
 */
@Component
public class UsuarioGatewayImpl implements UsuarioGateway {

    static final Duration DURACION_CACHE = Duration.ofSeconds(30);

    private final RestClient usuariosRestClient;
    private final Clock clock;
    private final Map<Long, Entrada> cache = new ConcurrentHashMap<>();

    private record Entrada(UsuarioInfo usuario, Instant vence) {
    }

    @Autowired
    public UsuarioGatewayImpl(@Qualifier("usuariosRestClient") RestClient usuariosRestClient, Clock clock) {
        this.usuariosRestClient = usuariosRestClient;
        this.clock = clock;
    }

    public UsuarioGatewayImpl(RestClient usuariosRestClient) {
        this(usuariosRestClient, Clock.systemUTC());
    }

    // Usuarios responde más campos (correo, rol); aquí solo se toman estos
    public record UsuarioResponse(Long idUsuario, String nombre, Boolean activo) {
    }

    @Override
    public Optional<UsuarioInfo> consultar(Long idUsuario) {
        Instant ahora = clock.instant();
        Entrada guardada = cache.get(idUsuario);
        if (guardada != null && ahora.isBefore(guardada.vence())) {
            return Optional.of(guardada.usuario());
        }
        Optional<UsuarioInfo> usuario = consultarServicio(idUsuario);
        usuario.ifPresentOrElse(
                encontrado -> cache.put(idUsuario, new Entrada(encontrado, ahora.plus(DURACION_CACHE))),
                () -> cache.remove(idUsuario));
        return usuario;
    }

    private Optional<UsuarioInfo> consultarServicio(Long idUsuario) {
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
