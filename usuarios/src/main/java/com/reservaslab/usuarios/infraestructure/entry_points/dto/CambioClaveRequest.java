package com.reservaslab.usuarios.infraestructure.entry_points.dto;

public record CambioClaveRequest(String claveActual, String claveNueva) {
}
