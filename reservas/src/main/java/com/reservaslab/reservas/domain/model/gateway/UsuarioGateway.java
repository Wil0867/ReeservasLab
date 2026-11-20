package com.reservaslab.reservas.domain.model.gateway;

import com.reservaslab.reservas.domain.model.UsuarioInfo;

import java.util.Optional;

/** Lo que Reservas necesita del microservicio Usuarios. */
public interface UsuarioGateway {
    /** Datos del usuario, o vacío si no existe. */
    Optional<UsuarioInfo> consultar(Long idUsuario);
}
