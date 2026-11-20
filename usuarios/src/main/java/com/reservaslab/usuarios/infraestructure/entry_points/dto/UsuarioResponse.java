package com.reservaslab.usuarios.infraestructure.entry_points.dto;

import com.reservaslab.usuarios.domain.model.Rol;

/** Lo que se responde de un usuario. No incluye la clave. */
public record UsuarioResponse(Long idUsuario, String nombre, String correo, Rol rol, Boolean activo) {
}
