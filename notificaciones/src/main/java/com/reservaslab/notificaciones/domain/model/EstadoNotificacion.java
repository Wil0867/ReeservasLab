package com.reservaslab.notificaciones.domain.model;

/**
 * PENDIENTE -> registrada; el correo se está enviando en segundo plano.
 * ENVIADA   -> el proveedor de correo la aceptó.
 * FALLIDA   -> el proveedor la rechazó; se puede reintentar.
 */
public enum EstadoNotificacion {
    PENDIENTE, ENVIADA, FALLIDA
}
