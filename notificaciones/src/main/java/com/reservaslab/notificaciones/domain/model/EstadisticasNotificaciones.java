package com.reservaslab.notificaciones.domain.model;

import java.util.Map;

/** Resumen del historial de notificaciones. */
public record EstadisticasNotificaciones(long total, long enviadas, long fallidas, long noLeidas,
                                         Map<TipoNotificacion, Long> porTipo) {
}
