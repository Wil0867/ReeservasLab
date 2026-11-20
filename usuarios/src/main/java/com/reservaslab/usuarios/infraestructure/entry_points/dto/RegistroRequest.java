package com.reservaslab.usuarios.infraestructure.entry_points.dto;

import com.reservaslab.usuarios.domain.model.Rol;

// Sin id ni estado: los decide el servidor
public record RegistroRequest(String nombre, String correo, String clave, Rol rol) {
}
