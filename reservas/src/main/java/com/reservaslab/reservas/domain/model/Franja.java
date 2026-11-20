package com.reservaslab.reservas.domain.model;

import java.time.LocalTime;

/** Una hora del día de un laboratorio y si está libre. */
public record Franja(LocalTime inicio, LocalTime fin, boolean disponible) {
}
