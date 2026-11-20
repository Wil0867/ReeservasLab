package com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository;

import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.domain.model.gateway.NotificacionGateway;
import com.reservaslab.notificaciones.infraestructure.mapper.MapperNotificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificacionDataGatewayImpl implements NotificacionGateway {

    private final NotificacionDataJpaRepository repository;
    private final MapperNotificacion mapperNotificacion;

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        return mapperNotificacion.toNotificacion(
                repository.save(mapperNotificacion.toNotificacionData(notificacion)));
    }

    @Override
    public Optional<Notificacion> buscarPorId(Long idNotificacion) {
        return repository.findById(idNotificacion).map(mapperNotificacion::toNotificacion);
    }

    @Override
    public List<Notificacion> listar() {
        return convertir(repository.findAll());
    }

    @Override
    public List<Notificacion> listarPorUsuario(Long idUsuario) {
        return convertir(repository.findByIdUsuarioOrderByFechaCreacionDesc(idUsuario));
    }

    @Override
    public List<Notificacion> listarNoLeidasPorUsuario(Long idUsuario) {
        return convertir(repository.findByIdUsuarioAndLeidaFalseOrderByFechaCreacionDesc(idUsuario));
    }

    @Override
    public List<Notificacion> listarPorEstado(EstadoNotificacion estado) {
        return convertir(repository.findByEstadoOrderByFechaCreacionDesc(estado));
    }

    @Override
    public void eliminar(Long idNotificacion) {
        repository.deleteById(idNotificacion);
    }

    private List<Notificacion> convertir(List<NotificacionData> datos) {
        return datos.stream().map(mapperNotificacion::toNotificacion).toList();
    }
}
