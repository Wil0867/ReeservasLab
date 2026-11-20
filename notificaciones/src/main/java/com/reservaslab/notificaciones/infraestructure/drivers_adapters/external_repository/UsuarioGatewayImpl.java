package com.reservaslab.notificaciones.infraestructure.drivers_adapters.external_repository;

import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import com.reservaslab.notificaciones.domain.model.gateway.UsuarioGateway;
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
 * Adaptador que le pregunta al microservicio Usuarios el correo de un usuario.
 * Guarda cada correo encontrado durante 30 s para no llamar a Usuarios en cada notificación.
 */
@Component
public class UsuarioGatewayImpl implements UsuarioGateway {

    static final Duration DURACION_CACHE = Duration.ofSeconds(30);

    private final RestClient usuariosRestClient;
    private final Clock clock;
    private final Map<Long, Entrada> cache = new ConcurrentHashMap<>();

    private record Entrada(String correo, Instant vence) {
    }

    @Autowired
    public UsuarioGatewayImpl(@Qualifier("usuariosRestClient") RestClient usuariosRestClient, Clock clock) {
        this.usuariosRestClient = usuariosRestClient;
        this.clock = clock;
    }

    public UsuarioGatewayImpl(RestClient usuariosRestClient) {
        this(usuariosRestClient, Clock.systemUTC());
    }

    // Usuarios responde más campos; aquí solo interesa el correo
    public record UsuarioResponse(Long idUsuario, String correo) {
    }

    @Override
    public Optional<String> consultarCorreo(Long idUsuario) {
        Instant ahora = clock.instant();
        Entrada guardada = cache.get(idUsuario);
        if (guardada != null && ahora.isBefore(guardada.vence())) {
            return Optional.of(guardada.correo());
        }
        Optional<String> correo = consultarServicio(idUsuario);
        correo.ifPresentOrElse(
                encontrado -> cache.put(idUsuario, new Entrada(encontrado, ahora.plus(DURACION_CACHE))),
                () -> cache.remove(idUsuario));
        return correo;
    }

    private Optional<String> consultarServicio(Long idUsuario) {
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
