package com.reservaslab.notificaciones.infraestructure.entry_points;

import com.reservaslab.notificaciones.domain.model.EstadisticasNotificaciones;
import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.domain.model.TipoNotificacion;
import com.reservaslab.notificaciones.domain.usecase.NotificacionUseCase;
import com.reservaslab.notificaciones.infraestructure.entry_points.dto.NotificacionRequest;
import com.reservaslab.notificaciones.infraestructure.mapper.MapperNotificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionControllerTest {

    private NotificacionUseCase useCase;
    private NotificacionController controller;
    private final Notificacion notificacion = new Notificacion();

    @BeforeEach
    void preparar() {
        useCase = mock(NotificacionUseCase.class);
        controller = new NotificacionController(useCase, new MapperNotificacion());
    }

    @Test
    void enviarResponde201() {
        when(useCase.enviar(any())).thenReturn(notificacion);

        ResponseEntity<Notificacion> respuesta = controller.enviar(
                new NotificacionRequest(1L, null, "Asunto", "Mensaje", TipoNotificacion.GENERAL));

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertSame(notificacion, respuesta.getBody());
    }

    @Test
    void listarBuscarYEliminar() {
        when(useCase.listar()).thenReturn(List.of(notificacion));
        when(useCase.buscar(5L)).thenReturn(notificacion);

        assertEquals(List.of(notificacion), controller.listar().getBody());
        assertSame(notificacion, controller.buscar(5L).getBody());

        ResponseEntity<Map<String, Object>> eliminada = controller.eliminar(5L);
        verify(useCase).eliminar(5L);
        assertEquals("Notificación eliminada correctamente", eliminada.getBody().get("mensaje"));
    }

    @Test
    void reintentarYMarcarLeida() {
        when(useCase.reintentar(5L)).thenReturn(notificacion);
        when(useCase.marcarLeida(5L)).thenReturn(notificacion);

        assertSame(notificacion, controller.reintentar(5L).getBody());
        assertSame(notificacion, controller.marcarLeida(5L).getBody());
    }

    @Test
    void listadosYEstadisticas() {
        EstadisticasNotificaciones estadisticas = new EstadisticasNotificaciones(1, 1, 0, 1, Map.of());
        when(useCase.listarPorUsuario(1L)).thenReturn(List.of(notificacion));
        when(useCase.listarNoLeidas(1L)).thenReturn(List.of(notificacion));
        when(useCase.listarFallidas()).thenReturn(List.of());
        when(useCase.estadisticas()).thenReturn(estadisticas);

        assertEquals(1, controller.historial(1L).getBody().size());
        assertEquals(1, controller.noLeidas(1L).getBody().size());
        assertEquals(0, controller.fallidas().getBody().size());
        assertSame(estadisticas, controller.estadisticas().getBody());
    }
}
