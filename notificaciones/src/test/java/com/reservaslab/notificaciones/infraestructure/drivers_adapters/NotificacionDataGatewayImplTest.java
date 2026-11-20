package com.reservaslab.notificaciones.infraestructure.drivers_adapters;

import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.domain.model.TipoNotificacion;
import com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository.NotificacionData;
import com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository.NotificacionDataGatewayImpl;
import com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository.NotificacionDataJpaRepository;
import com.reservaslab.notificaciones.infraestructure.mapper.MapperNotificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionDataGatewayImplTest {

    private NotificacionDataJpaRepository repository;
    private NotificacionDataGatewayImpl gateway;

    @BeforeEach
    void preparar() {
        repository = mock(NotificacionDataJpaRepository.class);
        gateway = new NotificacionDataGatewayImpl(repository, new MapperNotificacion());
    }

    private NotificacionData data() {
        return new NotificacionData(5L, 1L, "ana@correo.com", "Asunto", "Mensaje", TipoNotificacion.RESERVA,
                EstadoNotificacion.ENVIADA, false, 1, LocalDateTime.of(2026, 11, 16, 8, 0), null);
    }

    @Test
    void guardarDevuelveLoGuardado() {
        when(repository.save(any())).thenReturn(data());

        Notificacion guardada = gateway.guardar(new Notificacion());

        assertEquals(5L, guardada.getIdNotificacion());
    }

    @Test
    void buscarPorId() {
        when(repository.findById(5L)).thenReturn(Optional.of(data()));
        when(repository.findById(8L)).thenReturn(Optional.empty());

        assertEquals("Asunto", gateway.buscarPorId(5L).orElseThrow().getAsunto());
        assertTrue(gateway.buscarPorId(8L).isEmpty());
    }

    @Test
    void listados() {
        when(repository.findAll()).thenReturn(List.of(data(), data()));
        when(repository.findByIdUsuarioOrderByFechaCreacionDesc(1L)).thenReturn(List.of(data()));
        when(repository.findByIdUsuarioAndLeidaFalseOrderByFechaCreacionDesc(1L)).thenReturn(List.of(data()));
        when(repository.findByEstadoOrderByFechaCreacionDesc(EstadoNotificacion.FALLIDA)).thenReturn(List.of());

        assertEquals(2, gateway.listar().size());
        assertEquals(1, gateway.listarPorUsuario(1L).size());
        assertEquals(1, gateway.listarNoLeidasPorUsuario(1L).size());
        assertTrue(gateway.listarPorEstado(EstadoNotificacion.FALLIDA).isEmpty());
    }

    @Test
    void eliminar() {
        gateway.eliminar(5L);

        verify(repository).deleteById(5L);
    }
}
