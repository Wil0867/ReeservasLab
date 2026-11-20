package com.reservaslab.reservas.domain.usecase;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.EstadoLaboratorio;
import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.domain.model.UsuarioInfo;
import com.reservaslab.reservas.domain.model.gateway.FestivoGateway;
import com.reservaslab.reservas.domain.model.gateway.LaboratorioGateway;
import com.reservaslab.reservas.domain.model.gateway.NotificacionGateway;
import com.reservaslab.reservas.domain.model.gateway.ReservaGateway;
import com.reservaslab.reservas.domain.model.gateway.UsuarioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReservaUseCaseTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    // "Ahora" para las pruebas: lunes 16 de noviembre de 2026, 8:00 a. m.
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 8, 0);
    private static final LocalDate VIERNES = LocalDate.of(2026, 11, 20);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 22);

    private ReservaGateway reservaGateway;
    private LaboratorioGateway laboratorioGateway;
    private UsuarioGateway usuarioGateway;
    private FestivoGateway festivoGateway;
    private NotificacionGateway notificacionGateway;
    private ReservaUseCase useCase;

    @BeforeEach
    void preparar() {
        reservaGateway = mock(ReservaGateway.class);
        laboratorioGateway = mock(LaboratorioGateway.class);
        usuarioGateway = mock(UsuarioGateway.class);
        festivoGateway = mock(FestivoGateway.class);
        notificacionGateway = mock(NotificacionGateway.class);
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        useCase = new ReservaUseCase(reservaGateway, laboratorioGateway, usuarioGateway,
                festivoGateway, notificacionGateway, reloj);

        // Escenario feliz por defecto; cada prueba cambia solo lo que necesita
        when(usuarioGateway.consultar(1L)).thenReturn(Optional.of(new UsuarioInfo(1L, "Ana", true)));
        when(laboratorioGateway.buscarPorId(2L))
                .thenReturn(Optional.of(new Laboratorio(2L, "Lab Redes", "Bloque B", 20, EstadoLaboratorio.DISPONIBLE)));
        when(festivoGateway.esFestivo(any())).thenReturn(false);
        when(reservaGateway.crearSiNoHayCruce(any())).thenAnswer(invocacion -> {
            Reserva guardada = invocacion.getArgument(0);
            guardada.setIdReserva(10L);
            return Optional.of(guardada);
        });
    }

    private Reserva solicitud() {
        return new Reserva(77L, 1L, 2L, null, VIERNES, LocalTime.of(8, 0), LocalTime.of(10, 0),
                5, "  Práctica de redes  ", EstadoReserva.FINALIZADA);
    }

    private Reserva guardada(EstadoReserva estado) {
        return new Reserva(10L, 1L, 2L, "Lab Redes", VIERNES, LocalTime.of(8, 0), LocalTime.of(10, 0),
                5, "Práctica de redes", estado);
    }

    // ---------------- crear: camino feliz ----------------

    @Test
    void crearGuardaPendienteYNotifica() {
        Reserva creada = useCase.crear(solicitud());

        assertEquals(10L, creada.getIdReserva());
        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());        // el cliente no decide el estado
        assertEquals("Práctica de redes", creada.getMotivo());
        assertEquals("Lab Redes", creada.getNombreLaboratorio());
        verify(notificacionGateway).notificar(eq(1L), eq("Reserva registrada #10"), anyString());
    }

    @Test
    void crearNoFallaSiLaNotificacionFalla() {
        doThrow(new ServicioExternoException("caída", null))
                .when(notificacionGateway).notificar(anyLong(), anyString(), anyString());

        assertEquals(10L, useCase.crear(solicitud()).getIdReserva());
    }

    @Test
    void crearContinuaSiLaApiDeFestivosFalla() {
        when(festivoGateway.esFestivo(any())).thenThrow(new ServicioExternoException("caída", null));

        assertEquals(EstadoReserva.PENDIENTE, useCase.crear(solicitud()).getEstado());
    }

    // ---------------- crear: datos inválidos (400) ----------------

    @Test
    void crearRechazaReservaNula() {
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(null));
    }

    @Test
    void crearRechazaIdsInvalidos() {
        Reserva sinUsuario = solicitud();
        sinUsuario.setIdUsuario(null);
        Reserva usuarioCero = solicitud();
        usuarioCero.setIdUsuario(0L);
        Reserva sinLaboratorio = solicitud();
        sinLaboratorio.setIdLaboratorio(null);

        assertThrows(IllegalArgumentException.class, () -> useCase.crear(sinUsuario));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(usuarioCero));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(sinLaboratorio));
    }

    @Test
    void crearRechazaFechaUHorasInvalidas() {
        Reserva sinFecha = solicitud();
        sinFecha.setFecha(null);
        Reserva sinHora = solicitud();
        sinHora.setHoraInicio(null);
        Reserva horasAlReves = solicitud();
        horasAlReves.setHoraInicio(LocalTime.of(11, 0));

        assertThrows(IllegalArgumentException.class, () -> useCase.crear(sinFecha));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(sinHora));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(horasAlReves));
    }

    @Test
    void crearRechazaCantidadOMotivoInvalidos() {
        Reserva sinCantidad = solicitud();
        sinCantidad.setCantidadPersonas(null);
        Reserva cantidadCero = solicitud();
        cantidadCero.setCantidadPersonas(0);
        Reserva sinMotivo = solicitud();
        sinMotivo.setMotivo(null);
        Reserva motivoVacio = solicitud();
        motivoVacio.setMotivo("   ");

        assertThrows(IllegalArgumentException.class, () -> useCase.crear(sinCantidad));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(cantidadCero));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(sinMotivo));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(motivoVacio));
    }

    @Test
    void crearRechazaFechaUHoraPasadas() {
        Reserva ayer = solicitud();
        ayer.setFecha(AHORA.toLocalDate().minusDays(1));
        Reserva justoAhora = solicitud();
        justoAhora.setFecha(AHORA.toLocalDate());        // hoy a las 08:00 = "ahora", ya no es futuro

        assertThrows(IllegalArgumentException.class, () -> useCase.crear(ayer));
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(justoAhora));
        verify(reservaGateway, never()).crearSiNoHayCruce(any());
    }

    // ---------------- crear: reglas de negocio (404 / 409) ----------------

    @Test
    void crearRechazaDomingo() {
        Reserva domingo = solicitud();
        domingo.setFecha(DOMINGO);

        assertThrows(ReglaDeNegocioException.class, () -> useCase.crear(domingo));
    }

    @Test
    void crearRechazaUsuarioInexistenteOInactivo() {
        Reserva otroUsuario = solicitud();
        otroUsuario.setIdUsuario(9L);
        when(usuarioGateway.consultar(9L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.crear(otroUsuario));

        when(usuarioGateway.consultar(1L)).thenReturn(Optional.of(new UsuarioInfo(1L, "Ana", false)));
        assertThrows(ReglaDeNegocioException.class, () -> useCase.crear(solicitud()));
    }

    @Test
    void crearRechazaLaboratorioInexistente() {
        when(laboratorioGateway.buscarPorId(2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> useCase.crear(solicitud()));
    }

    @Test
    void crearRechazaLaboratorioEnMantenimiento() {
        when(laboratorioGateway.buscarPorId(2L)).thenReturn(Optional.of(
                new Laboratorio(2L, "Lab Redes", "Bloque B", 20, EstadoLaboratorio.MANTENIMIENTO)));

        assertThrows(ReglaDeNegocioException.class, () -> useCase.crear(solicitud()));
    }

    @Test
    void crearRechazaSiSuperaLaCapacidad() {
        Reserva grupoGrande = solicitud();
        grupoGrande.setCantidadPersonas(21);

        assertThrows(ReglaDeNegocioException.class, () -> useCase.crear(grupoGrande));
    }

    @Test
    void crearRechazaFestivo() {
        when(festivoGateway.esFestivo(VIERNES)).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> useCase.crear(solicitud()));
        verify(reservaGateway, never()).crearSiNoHayCruce(any());
    }

    @Test
    void crearRechazaHorarioOcupado() {
        // doReturn no ejecuta el stub del @BeforeEach (que fallaría con argumento null)
        doReturn(Optional.empty()).when(reservaGateway).crearSiNoHayCruce(any());

        assertThrows(ReglaDeNegocioException.class, () -> useCase.crear(solicitud()));
        verify(notificacionGateway, never()).notificar(anyLong(), anyString(), anyString());
    }

    // ---------------- buscar / historial ----------------

    @Test
    void buscarDevuelveLaReserva() {
        Reserva existente = guardada(EstadoReserva.PENDIENTE);
        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(existente));

        assertSame(existente, useCase.buscar(10L));
    }

    @Test
    void buscarRechazaIdInvalidoONoEncontrado() {
        when(reservaGateway.buscarPorId(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(-1L));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.buscar(5L));
    }

    @Test
    void listarPorUsuarioDevuelveElHistorial() {
        List<Reserva> historial = List.of(guardada(EstadoReserva.CANCELADA));
        when(reservaGateway.listarPorUsuario(1L)).thenReturn(historial);

        assertEquals(historial, useCase.listarPorUsuario(1L));
        assertThrows(IllegalArgumentException.class, () -> useCase.listarPorUsuario(0L));
    }

    // ---------------- ciclo de vida ----------------

    @Test
    void confirmarPasaDePendienteAConfirmada() {
        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.PENDIENTE)));
        when(reservaGateway.actualizarEstado(10L, EstadoReserva.CONFIRMADA))
                .thenReturn(guardada(EstadoReserva.CONFIRMADA));

        assertEquals(EstadoReserva.CONFIRMADA, useCase.confirmar(10L).getEstado());
        verify(notificacionGateway).notificar(eq(1L), eq("Reserva confirmada #10"), anyString());
    }

    @Test
    void confirmarRechazaSiNoEstaPendiente() {
        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.CANCELADA)));

        assertThrows(ReglaDeNegocioException.class, () -> useCase.confirmar(10L));
        verify(reservaGateway, never()).actualizarEstado(anyLong(), any());
    }

    @Test
    void cancelarFuncionaDesdePendienteYDesdeConfirmada() {
        when(reservaGateway.actualizarEstado(10L, EstadoReserva.CANCELADA))
                .thenReturn(guardada(EstadoReserva.CANCELADA));

        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.PENDIENTE)));
        assertEquals(EstadoReserva.CANCELADA, useCase.cancelar(10L).getEstado());

        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.CONFIRMADA)));
        assertEquals(EstadoReserva.CANCELADA, useCase.cancelar(10L).getEstado());
    }

    @Test
    void cancelarRechazaUnaReservaFinalizada() {
        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.FINALIZADA)));

        assertThrows(ReglaDeNegocioException.class, () -> useCase.cancelar(10L));
    }

    @Test
    void finalizarSoloDesdeConfirmada() {
        when(reservaGateway.actualizarEstado(10L, EstadoReserva.FINALIZADA))
                .thenReturn(guardada(EstadoReserva.FINALIZADA));

        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.CONFIRMADA)));
        assertEquals(EstadoReserva.FINALIZADA, useCase.finalizar(10L).getEstado());

        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.PENDIENTE)));
        assertThrows(ReglaDeNegocioException.class, () -> useCase.finalizar(10L));
    }

    @Test
    void cambioDeEstadoNoFallaSiLaNotificacionFalla() {
        when(reservaGateway.buscarPorId(10L)).thenReturn(Optional.of(guardada(EstadoReserva.PENDIENTE)));
        when(reservaGateway.actualizarEstado(10L, EstadoReserva.CONFIRMADA))
                .thenReturn(guardada(EstadoReserva.CONFIRMADA));
        doThrow(new RuntimeException("sin conexión"))
                .when(notificacionGateway).notificar(anyLong(), anyString(), anyString());

        assertEquals(EstadoReserva.CONFIRMADA, useCase.confirmar(10L).getEstado());
    }

    // ---------------- festivo ----------------

    @Test
    void consultarFestivoDevuelveLoQueDiceLaApi() {
        LocalDate navidad = LocalDate.of(2026, 12, 25);
        when(festivoGateway.esFestivo(navidad)).thenReturn(true);

        assertTrue(useCase.consultarFestivo(navidad));
        assertFalse(useCase.consultarFestivo(VIERNES));
    }

    @Test
    void consultarFestivoRechazaFechaNulaYPropagaLaCaidaDeLaApi() {
        when(festivoGateway.esFestivo(VIERNES)).thenThrow(new ServicioExternoException("caída", null));

        assertThrows(IllegalArgumentException.class, () -> useCase.consultarFestivo(null));
        assertThrows(ServicioExternoException.class, () -> useCase.consultarFestivo(VIERNES));
    }
}
