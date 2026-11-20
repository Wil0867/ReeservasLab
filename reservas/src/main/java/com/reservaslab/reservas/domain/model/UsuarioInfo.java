package com.reservaslab.reservas.domain.model;

/** Lo único que Reservas necesita saber de un usuario. */
public record UsuarioInfo(Long idUsuario, String nombre, boolean activo) {
}
