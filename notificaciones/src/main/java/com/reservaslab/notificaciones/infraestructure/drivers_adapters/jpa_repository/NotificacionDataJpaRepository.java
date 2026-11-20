package com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository;

import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionDataJpaRepository extends JpaRepository<NotificacionData, Long> {

    List<NotificacionData> findByIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);

    List<NotificacionData> findByIdUsuarioAndLeidaFalseOrderByFechaCreacionDesc(Long idUsuario);

    List<NotificacionData> findByEstadoOrderByFechaCreacionDesc(EstadoNotificacion estado);
}
