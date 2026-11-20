package com.reservaslab.notificaciones.domain.usecase;

import com.reservaslab.notificaciones.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.notificaciones.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import com.reservaslab.notificaciones.domain.model.EstadisticasNotificaciones;
import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.domain.model.TipoNotificacion;
import com.reservaslab.notificaciones.domain.model.gateway.CorreoGateway;
import com.reservaslab.notificaciones.domain.model.gateway.NotificacionGateway;
import com.reservaslab.notificaciones.domain.model.gateway.UsuarioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificacionUseCaseTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 8, 0);

    private NotificacionGateway notificacionGateway;
    private CorreoGateway correoGateway;
    private UsuarioGateway usuarioGateway;
    private NotificacionUseCase useCase;

    @BeforeEach
    void preparar() {
        notificacionGateway = mock(NotificacionGateway.class);
        correoGateway = mock(CorreoGateway.class);
        usuarioGateway = mock(UsuarioGateway.class);
        useCase = new NotificacionUseCase(notificacionGateway, correoGateway, usuarioGateway,
                Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA));
        when(notificacionGateway.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Notificacion solicitud(String destinatario) {
        Notificacion solicitud = new Notificacion();
        solicitud.setIdNotificacion(99L);
        solicitud.setIdUsuario(1L);
        solicitud.setDestinatario(destinatario);
        solicitud.setAsunto("  Reserva registrada #10 ");
        solicitud.setMensaje(" Detalle de la reserva ");
        solicitud.setTipo(TipoNotificacion.RESERVA);
        return solicitud;
    }

    private Notificacion guardada(EstadoNotificacion estado, boolean leida, int intentos, TipoNotificacion tipo) {
        return new Notificacion(5L, 1L, "ana@correo.com", "Asunto", "Mensaje", tipo, estado, leida,
                intentos, AHORA, estado == EstadoNotificacion.FALLIDA ? "error" : null);
    }

    // ---------------- enviar ----------------

    @Test
    void enviarRegistraPendienteSinLlamarAlProveedor() {
        Notificacion resultado = useCase.enviar(solicitud(" ana@correo.com "));

        assertNull(resultado.getIdNotificacion());
        assertEquals("ana@correo.com", resultado.getDestinatario());
        assertEquals("Reserva registrada #10", resultado.getAsunto());
        assertEquals("Detalle de la reserva", resultado.getMensaje());
        assertEquals(EstadoNotificacion.PENDIENTE, resultado.getEstado());
        assertEquals(0, resultado.getIntentos());
        assertFalse(resultado.getLeida());
        assertEquals(AHORA, resultado.getFechaCreacion());
        assertNull(resultado.getDetalleError());
        verify(notificacionGateway).guardar(resultado);
        verify(correoGateway, never()).enviar(anyString(), anyString(), anyString());
        verify(usuarioGateway, never()).consultarCorreo(anyLong());
    }

    @Test
    void enviarSinDestinatarioUsaElCorreoRegistrado() {
        when(usuarioGateway.consultarCorreo(1L)).thenReturn(Optional.of("registrado@correo.com"));

        assertEquals("registrado@correo.com", useCase.enviar(solicitud(null)).getDestinatario());
        assertEquals("registrado@correo.com", useCase.enviar(solicitud("   ")).getDestinatario());
    }

    @Test
    void enviarSinTipoQuedaComoGeneral() {
        Notificacion sinTipo = solicitud("ana@correo.com");
        sinTipo.setTipo(null);

        assertEquals(TipoNotificacion.GENERAL, useCase.enviar(sinTipo).getTipo());
    }

    // ---------------- procesarEnvio (segundo plano) ----------------

    @Test
    void procesarEnvioEnviaYMarcaEnviada() {
        Notificacion pendiente = guardada(EstadoNotificacion.PENDIENTE, false, 0, TipoNotificacion.RESERVA);
        when(notificacionGateway.buscarPorId(5L)).thenReturn(Optional.of(pendiente));

        Notificacion resultado = useCase.procesarEnvio(5L);

        assertEquals(EstadoNotificacion.ENVIADA, resultado.getEstado());
        assertEquals(1, resultado.getIntentos());
        assertNull(resultado.getDetalleError());
        verify(correoGateway).enviar("ana@correo.com", "Asunto", "Mensaje");
        verify(notificacionGateway).guardar(pendiente);
    }

    @Test
    void procesarEnvioRegistraFallidaSiElProveedorRechaza() {
        when(notificacionGateway.buscarPorId(5L))
                .thenReturn(Optional.of(guardada(EstadoNotificacion.PENDIENTE, false, 0, TipoNotificacion.RESERVA)));
        doThrow(new ServicioExternoException("x".repeat(600), null))
                .when(correoGateway).enviar(anyString(), anyString(), anyString());

        Notificacion resultado = useCase.procesarEnvio(5L);

        assertEquals(EstadoNotificacion.FALLIDA, resultado.getEstado());
        assertEquals(1, resultado.getIntentos());
        assertEquals(500, resultado.getDetalleError().length());     // el error se recorta para la columna
        verify(notificacionGateway).guardar(resultado);              // igual queda en el historial
    }

    @Test
    void procesarEnvioNoHaceNadaSiNoEstaPendiente() {
        Notificacion enviada = guardada(EstadoNotificacion.ENVIADA, false, 1, TipoNotificacion.RESERVA);
        when(notificacionGateway.buscarPorId(5L)).thenReturn(Optional.of(enviada));

        assertSame(enviada, useCase.procesarEnvio(5L));
        verify(correoGateway, never()).enviar(anyString(), anyString(), anyString());
        verify(notificacionGateway, never()).guardar(any());
    }

    @Test
    void enviarRechazaDatosInvalidos() {
        Notificacion sinUsuario = solicitud("ana@correo.com");
        sinUsuario.setIdUsuario(null);
        Notificacion usuarioCero = solicitud("ana@correo.com");
        usuarioCero.setIdUsuario(0L);
        Notificacion sinAsunto = solicitud("ana@correo.com");
        sinAsunto.setAsunto("  ");
        Notificacion sinMensaje = solicitud("ana@correo.com");
        sinMensaje.setMensaje(null);

        assertThrows(IllegalArgumentException.class, () -> useCase.enviar(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.enviar(sinUsuario));
        assertThrows(IllegalArgumentException.class, () -> useCase.enviar(usuarioCero));
        assertThrows(IllegalArgumentException.class, () -> useCase.enviar(sinAsunto));
        assertThrows(IllegalArgumentException.class, () -> useCase.enviar(sinMensaje));
        assertThrows(IllegalArgumentException.class, () -> useCase.enviar(solicitud("correo-sin-arroba")));
        verify(correoGateway, never()).enviar(anyString(), anyString(), anyString());
        verify(notificacionGateway, never()).guardar(any());
    }

    @Test
    void enviarRechazaUsuarioInexistente() {
        when(usuarioGateway.consultarCorreo(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> useCase.enviar(solicitud(null)));
    }

    // ---------------- reintentar ----------------

    @Test
    void reintentarEnviaYSumaElIntento() {
        when(notificacionGateway.buscarPorId(5L))
                .thenReturn(Optional.of(guardada(EstadoNotificacion.FALLIDA, false, 1, TipoNotificacion.RESERVA)));

        Notificacion resultado = useCase.reintentar(5L);

        assertEquals(EstadoNotificacion.ENVIADA, resultado.getEstado());
        assertEquals(2, resultado.getIntentos());
        assertNull(resultado.getDetalleError());
    }

    @Test
    void reintentarQueVuelveAFallarSigueFallida() {
        when(notificacionGateway.buscarPorId(5L))
                .thenReturn(Optional.of(guardada(EstadoNotificacion.FALLIDA, false, 2, TipoNotificacion.RESERVA)));
        doThrow(new ServicioExternoException("rechazado", null))
                .when(correoGateway).enviar(anyString(), anyString(), anyString());

        Notificacion resultado = useCase.reintentar(5L);

        assertEquals(EstadoNotificacion.FALLIDA, resultado.getEstado());
        assertEquals(3, resultado.getIntentos());
        assertEquals("rechazado", resultado.getDetalleError());
    }

    @Test
    void reintentarRechazaEnviadasYMaximoDeIntentos() {
        when(notificacionGateway.buscarPorId(5L))
                .thenReturn(Optional.of(guardada(EstadoNotificacion.ENVIADA, false, 1, TipoNotificacion.RESERVA)));
        assertThrows(ReglaDeNegocioException.class, () -> useCase.reintentar(5L));

        when(notificacionGateway.buscarPorId(5L))
                .thenReturn(Optional.of(guardada(EstadoNotificacion.FALLIDA, false, 3, TipoNotificacion.RESERVA)));
        assertThrows(ReglaDeNegocioException.class, () -> useCase.reintentar(5L));

        verify(correoGateway, never()).enviar(anyString(), anyString(), anyString());
    }

    // ---------------- buscar / listar / eliminar ----------------

    @Test
    void buscarDevuelveORechaza() {
        Notificacion existente = guardada(EstadoNotificacion.ENVIADA, false, 1, TipoNotificacion.GENERAL);
        when(notificacionGateway.buscarPorId(5L)).thenReturn(Optional.of(existente));
        when(notificacionGateway.buscarPorId(8L)).thenReturn(Optional.empty());

        assertSame(existente, useCase.buscar(5L));
        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(0L));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.buscar(8L));
    }

    @Test
    void listarYEliminar() {
        Notificacion existente = guardada(EstadoNotificacion.ENVIADA, false, 1, TipoNotificacion.GENERAL);
        when(notificacionGateway.listar()).thenReturn(List.of(existente));
        when(notificacionGateway.buscarPorId(5L)).thenReturn(Optional.of(existente));

        assertEquals(List.of(existente), useCase.listar());
        useCase.eliminar(5L);
        verify(notificacionGateway).eliminar(5L);
    }

    // ---------------- leída ----------------

    @Test
    void marcarLeidaCambiaElIndicador() {
        when(notificacionGateway.buscarPorId(5L))
                .thenReturn(Optional.of(guardada(EstadoNotificacion.ENVIADA, false, 1, TipoNotificacion.GENERAL)));

        assertTrue(useCase.marcarLeida(5L).getLeida());
    }

    @Test
    void marcarLeidaRechazaSiYaEstabaLeida() {
        when(notificacionGateway.buscarPorId(5L))
                .thenReturn(Optional.of(guardada(EstadoNotificacion.ENVIADA, true, 1, TipoNotificacion.GENERAL)));

        assertThrows(ReglaDeNegocioException.class, () -> useCase.marcarLeida(5L));
        verify(notificacionGateway, never()).guardar(any());
    }

    // ---------------- listados de negocio ----------------

    @Test
    void listadosPorUsuarioNoLeidasYFallidas() {
        List<Notificacion> lista = List.of(guardada(EstadoNotificacion.FALLIDA, false, 1, TipoNotificacion.CUENTA));
        when(notificacionGateway.listarPorUsuario(1L)).thenReturn(lista);
        when(notificacionGateway.listarNoLeidasPorUsuario(1L)).thenReturn(lista);
        when(notificacionGateway.listarPorEstado(EstadoNotificacion.FALLIDA)).thenReturn(lista);

        assertEquals(lista, useCase.listarPorUsuario(1L));
        assertEquals(lista, useCase.listarNoLeidas(1L));
        assertEquals(lista, useCase.listarFallidas());
        assertThrows(IllegalArgumentException.class, () -> useCase.listarPorUsuario(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.listarNoLeidas(-3L));
    }

    @Test
    void estadisticasCuentaPorEstadoYPorTipo() {
        Notificacion sinIndicador = guardada(EstadoNotificacion.ENVIADA, false, 1, TipoNotificacion.GENERAL);
        sinIndicador.setLeida(null);                    // un registro sin el dato cuenta como no leído
        when(notificacionGateway.listar()).thenReturn(List.of(
                guardada(EstadoNotificacion.ENVIADA, true, 1, TipoNotificacion.RESERVA),
                guardada(EstadoNotificacion.ENVIADA, false, 1, TipoNotificacion.RESERVA),
                guardada(EstadoNotificacion.FALLIDA, false, 3, TipoNotificacion.CUENTA),
                guardada(EstadoNotificacion.PENDIENTE, false, 0, TipoNotificacion.GENERAL),
                sinIndicador));

        EstadisticasNotificaciones estadisticas = useCase.estadisticas();

        assertEquals(5, estadisticas.total());
        assertEquals(3, estadisticas.enviadas());
        assertEquals(1, estadisticas.fallidas());
        assertEquals(1, estadisticas.pendientes());
        assertEquals(4, estadisticas.noLeidas());
        assertEquals(2L, estadisticas.porTipo().get(TipoNotificacion.RESERVA));
        assertEquals(1L, estadisticas.porTipo().get(TipoNotificacion.CUENTA));
        assertEquals(0L, estadisticas.porTipo().get(TipoNotificacion.RECORDATORIO));
        assertEquals(2L, estadisticas.porTipo().get(TipoNotificacion.GENERAL));
    }
}
