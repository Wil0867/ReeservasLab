package com.reservaslab.notificaciones.infraestructure.entry_points.dto;

import com.reservaslab.notificaciones.domain.model.TipoNotificacion;

// destinatario y tipo son opcionales: sin destinatario se usa el correo registrado del usuario
public record NotificacionRequest(Long idUsuario, String destinatario, String asunto, String mensaje,
                                  TipoNotificacion tipo) {
}
