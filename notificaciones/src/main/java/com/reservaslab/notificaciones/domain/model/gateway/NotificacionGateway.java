package com.reservaslab.notificaciones.domain.model.gateway;

import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import com.reservaslab.notificaciones.domain.model.Notificacion;

import java.util.List;
import java.util.Optional;

/** Historial de notificaciones (base de datos). */
public interface NotificacionGateway {

    Notificacion guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorId(Long idNotificacion);

    List<Notificacion> listar();

    List<Notificacion> listarPorUsuario(Long idUsuario);

    List<Notificacion> listarNoLeidasPorUsuario(Long idUsuario);

    List<Notificacion> listarPorEstado(EstadoNotificacion estado);

    void eliminar(Long idNotificacion);
}
