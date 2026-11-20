package com.reservaslab.reservas.domain.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/** Reglas de horario que comparten los casos de uso. */
public final class ReglasReserva {

    public static final LocalTime APERTURA = LocalTime.of(7, 0);
    public static final LocalTime CIERRE = LocalTime.of(21, 0);
    public static final long DURACION_MAXIMA_HORAS = 4;

    /** Estados en los que una reserva ocupa el laboratorio. */
    public static final List<EstadoReserva> ESTADOS_ACTIVOS =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private ReglasReserva() {
    }

    public static void validarFranja(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("La hora de inicio y la hora de fin son obligatorias (formato HH:mm)");
        }
        if (!horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (horaInicio.isBefore(APERTURA) || horaFin.isAfter(CIERRE)) {
            throw new IllegalArgumentException("Los laboratorios atienden de " + APERTURA + " a " + CIERRE);
        }
        if (Duration.between(horaInicio, horaFin).toMinutes() > DURACION_MAXIMA_HORAS * 60) {
            throw new IllegalArgumentException(
                    "Una reserva no puede durar más de " + DURACION_MAXIMA_HORAS + " horas");
        }
    }
}
