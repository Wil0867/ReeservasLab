package com.reservaslab.reservas.domain.model;

/**
 * PENDIENTE  -> recién creada, falta que la confirmen.
 * CONFIRMADA -> aprobada; el laboratorio queda apartado.
 * CANCELADA  -> ya no ocupa el horario.
 * FINALIZADA -> la práctica se realizó.
 */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    FINALIZADA
}
