package com.reservaslab.notificaciones.infraestructure.mapper;

import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository.NotificacionData;
import com.reservaslab.notificaciones.infraestructure.entry_points.dto.NotificacionRequest;
import org.springframework.stereotype.Component;

@Component
public class MapperNotificacion {

    public Notificacion toNotificacion(NotificacionData data) {
        if (data == null) return null;
        return new Notificacion(data.getIdNotificacion(), data.getIdUsuario(), data.getDestinatario(),
                data.getAsunto(), data.getMensaje(), data.getTipo(), data.getEstado(), data.getLeida(),
                data.getIntentos(), data.getFechaCreacion(), data.getDetalleError());
    }

    public NotificacionData toNotificacionData(Notificacion notificacion) {
        if (notificacion == null) return null;
        return new NotificacionData(notificacion.getIdNotificacion(), notificacion.getIdUsuario(),
                notificacion.getDestinatario(), notificacion.getAsunto(), notificacion.getMensaje(),
                notificacion.getTipo(), notificacion.getEstado(), notificacion.getLeida(),
                notificacion.getIntentos(), notificacion.getFechaCreacion(), notificacion.getDetalleError());
    }

    /** El cliente solo decide a quién, qué y de qué tipo. Lo demás lo pone el caso de uso. */
    public Notificacion toNotificacion(NotificacionRequest request) {
        if (request == null) return null;
        Notificacion notificacion = new Notificacion();
        notificacion.setIdUsuario(request.idUsuario());
        notificacion.setDestinatario(request.destinatario());
        notificacion.setAsunto(request.asunto());
        notificacion.setMensaje(request.mensaje());
        notificacion.setTipo(request.tipo());
        return notificacion;
    }
}
