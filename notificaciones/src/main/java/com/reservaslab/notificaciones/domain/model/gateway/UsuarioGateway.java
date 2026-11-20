package com.reservaslab.notificaciones.domain.model.gateway;

import java.util.Optional;

/** Lo que Notificaciones necesita del microservicio Usuarios: el correo de un usuario. */
public interface UsuarioGateway {
    Optional<String> consultarCorreo(Long idUsuario);
}
