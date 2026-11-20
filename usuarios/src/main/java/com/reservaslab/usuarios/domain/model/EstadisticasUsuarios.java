package com.reservaslab.usuarios.domain.model;

import java.util.Map;

/** Resumen de los usuarios registrados. */
public record EstadisticasUsuarios(long total, long activos, long inactivos, Map<Rol, Long> porRol) {
}
