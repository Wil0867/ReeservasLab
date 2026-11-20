package com.reservaslab.reservas.infraestructure.entry_points.dto;

import java.time.LocalDate;
import java.time.LocalTime;

// fecha "2026-11-20", horas "08:00"
public record ReservaRequest(Long idUsuario, Long idLaboratorio, LocalDate fecha, LocalTime horaInicio,
                             LocalTime horaFin, Integer cantidadPersonas, String motivo) {
}
