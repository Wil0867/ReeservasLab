package com.reservaslab.notificaciones.infraestructure.mapper;

import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.domain.model.TipoNotificacion;
import com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository.NotificacionData;
import com.reservaslab.notificaciones.infraestructure.entry_points.dto.NotificacionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperNotificacionTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 16, 8, 0);

    private final MapperNotificacion mapper = new MapperNotificacion();

    @Test
    void deDataADominioYAlReves() {
        NotificacionData data = new NotificacionData(5L, 1L, "ana@correo.com", "Asunto", "Mensaje",
                TipoNotificacion.RESERVA, EstadoNotificacion.FALLIDA, true, 2, FECHA, "error");

        Notificacion notificacion = mapper.toNotificacion(data);
        assertEquals(5L, notificacion.getIdNotificacion());
        assertEquals(1L, notificacion.getIdUsuario());
        assertEquals("ana@correo.com", notificacion.getDestinatario());
        assertEquals("Asunto", notificacion.getAsunto());
        assertEquals("Mensaje", notificacion.getMensaje());
        assertEquals(TipoNotificacion.RESERVA, notificacion.getTipo());
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
        assertTrue(notificacion.getLeida());
        assertEquals(2, notificacion.getIntentos());
        assertEquals(FECHA, notificacion.getFechaCreacion());
        assertEquals("error", notificacion.getDetalleError());

        assertEquals(data, mapper.toNotificacionData(notificacion));
    }

    @Test
    void desdeElRequestSoloTraeLoQueDecideElCliente() {
        Notificacion notificacion = mapper.toNotificacion(
                new NotificacionRequest(1L, "ana@correo.com", "Asunto", "Mensaje", TipoNotificacion.CUENTA));

        assertEquals(1L, notificacion.getIdUsuario());
        assertEquals("ana@correo.com", notificacion.getDestinatario());
        assertEquals("Asunto", notificacion.getAsunto());
        assertEquals("Mensaje", notificacion.getMensaje());
        assertEquals(TipoNotificacion.CUENTA, notificacion.getTipo());
        assertNull(notificacion.getIdNotificacion());
        assertNull(notificacion.getEstado());
    }

    @Test
    void losNulosSeDevuelvenComoNulos() {
        assertNull(mapper.toNotificacion((NotificacionData) null));
        assertNull(mapper.toNotificacion((NotificacionRequest) null));
        assertNull(mapper.toNotificacionData(null));
    }
}
