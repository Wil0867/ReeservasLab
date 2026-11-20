package com.reservaslab.usuarios.infraestructure.entry_points.dto;

import com.reservaslab.usuarios.domain.model.Rol;

// Sin clave: la clave solo se cambia con su propia operación
public record ActualizarRequest(String nombre, String correo, Rol rol) {
}
