package com.reservaslab.reservas.domain.usecase;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.Disponibilidad;
import com.reservaslab.reservas.domain.model.EstadoLaboratorio;
import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.domain.model.gateway.FestivoGateway;
import com.reservaslab.reservas.domain.model.gateway.LaboratorioGateway;
import com.reservaslab.reservas.domain.model.gateway.ReservaGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LaboratorioUseCaseTest {

    private static final LocalDate VIERNES = LocalDate.of(2026, 11, 20);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 22);

    private LaboratorioGateway laboratorioGateway;
    private ReservaGateway reservaGateway;
    private FestivoGateway festivoGateway;
    private LaboratorioUseCase useCase;

    @BeforeEach
    void preparar() {
        laboratorioGateway = mock(LaboratorioGateway.class);
        reservaGateway = mock(ReservaGateway.class);
        festivoGateway = mock(FestivoGateway.class);
        useCase = new LaboratorioUseCase(laboratorioGateway, reservaGateway, festivoGateway);
        when(laboratorioGateway.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Laboratorio laboratorio(Long id, EstadoLaboratorio estado) {
        return new Laboratorio(id, "Lab Redes", "Bloque B", 20, estado);
    }

    private Reserva reserva(int desde, int hasta) {
        return new Reserva(1L, 1L, 1L, "Lab Redes", VIERNES, LocalTime.of(desde, 0), LocalTime.of(hasta, 0),
                5, "Práctica", EstadoReserva.CONFIRMADA);
    }

    // ---------------- crear ----------------

    @Test
    void crearGuardaLimpioYDisponible() {
        Laboratorio creado = useCase.crear(new Laboratorio(99L, "  Lab Redes ", " Bloque B ", 20, null));

        assertNull(creado.getIdLaboratorio());
        assertEquals("Lab Redes", creado.getNombre());
        assertEquals("Bloque B", creado.getUbicacion());
        assertEquals(EstadoLaboratorio.DISPONIBLE, creado.getEstado());
        verify(laboratorioGateway).guardar(creado);
    }

    @Test
    void crearRechazaDatosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> useCase.crear(null));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.crear(new Laboratorio(null, null, "Bloque B", 20, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.crear(new Laboratorio(null, "   ", "Bloque B", 20, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.crear(new Laboratorio(null, "Lab", null, 20, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.crear(new Laboratorio(null, "Lab", "  ", 20, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.crear(new Laboratorio(null, "Lab", "Bloque B", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.crear(new Laboratorio(null, "Lab", "Bloque B", 0, null)));
        verify(laboratorioGateway, never()).guardar(any());
    }

    // ---------------- buscar / listar ----------------

    @Test
    void buscarDevuelveElLaboratorio() {
        Laboratorio existente = laboratorio(1L, EstadoLaboratorio.DISPONIBLE);
        when(laboratorioGateway.buscarPorId(1L)).thenReturn(Optional.of(existente));

        assertSame(existente, useCase.buscar(1L));
    }

    @Test
    void buscarRechazaIdInvalidoONoEncontrado() {
        when(laboratorioGateway.buscarPorId(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(0L));
        assertThrows(RecursoNoEncontradoException.class, () -> useCase.buscar(7L));
    }

    @Test
    void listarDevuelveTodos() {
        List<Laboratorio> todos = List.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE));
        when(laboratorioGateway.listar()).thenReturn(todos);

        assertEquals(todos, useCase.listar());
    }

    // ---------------- actualizar / eliminar ----------------

    @Test
    void actualizarCambiaLosDatosPeroNoElEstado() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.MANTENIMIENTO)));

        Laboratorio actualizado = useCase.actualizar(1L,
                new Laboratorio(null, " Lab IoT ", " Bloque C ", 30, EstadoLaboratorio.DISPONIBLE));

        assertEquals("Lab IoT", actualizado.getNombre());
        assertEquals("Bloque C", actualizado.getUbicacion());
        assertEquals(30, actualizado.getCapacidad());
        assertEquals(EstadoLaboratorio.MANTENIMIENTO, actualizado.getEstado());
    }

    @Test
    void actualizarRechazaDatosNulos() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));

        assertThrows(IllegalArgumentException.class, () -> useCase.actualizar(1L, null));
    }

    @Test
    void eliminarBorraSiNoTieneReservasActivas() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));
        when(reservaGateway.existenActivasPorLaboratorio(1L)).thenReturn(false);

        useCase.eliminar(1L);

        verify(laboratorioGateway).eliminar(1L);
    }

    @Test
    void eliminarRechazaSiTieneReservasActivas() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));
        when(reservaGateway.existenActivasPorLaboratorio(1L)).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> useCase.eliminar(1L));
        verify(laboratorioGateway, never()).eliminar(any());
    }

    // ---------------- cambiarEstado ----------------

    @Test
    void cambiarEstadoPoneEnMantenimiento() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));

        Laboratorio resultado = useCase.cambiarEstado(1L, EstadoLaboratorio.MANTENIMIENTO);

        assertEquals(EstadoLaboratorio.MANTENIMIENTO, resultado.getEstado());
    }

    @Test
    void cambiarEstadoRechazaNuloOElMismoEstado() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));

        assertThrows(IllegalArgumentException.class, () -> useCase.cambiarEstado(1L, null));
        assertThrows(ReglaDeNegocioException.class,
                () -> useCase.cambiarEstado(1L, EstadoLaboratorio.DISPONIBLE));
        verify(laboratorioGateway, never()).guardar(any());
    }

    // ---------------- disponibilidad ----------------

    @Test
    void disponibilidadMarcaOcupadasLasHorasReservadas() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));
        when(festivoGateway.esFestivo(VIERNES)).thenReturn(false);
        when(reservaGateway.listarActivasPorLaboratorioYFecha(1L, VIERNES)).thenReturn(List.of(reserva(8, 10)));

        Disponibilidad disponibilidad = useCase.consultarDisponibilidad(1L, VIERNES);

        assertEquals(14, disponibilidad.franjas().size());           // de 07:00 a 21:00
        assertTrue(disponibilidad.laboratorioHabilitado());
        assertFalse(disponibilidad.festivo());
        assertTrue(disponibilidad.franjas().get(0).disponible());    // 07-08 libre
        assertFalse(disponibilidad.franjas().get(1).disponible());   // 08-09 ocupada
        assertFalse(disponibilidad.franjas().get(2).disponible());   // 09-10 ocupada
        assertTrue(disponibilidad.franjas().get(3).disponible());    // 10-11 libre
        assertEquals(LocalTime.of(20, 0), disponibilidad.franjas().get(13).inicio());
        assertEquals(LocalTime.of(21, 0), disponibilidad.franjas().get(13).fin());
    }

    @Test
    void disponibilidadEnFestivoNoHabilitaNingunaFranja() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));
        when(festivoGateway.esFestivo(VIERNES)).thenReturn(true);
        when(reservaGateway.listarActivasPorLaboratorioYFecha(1L, VIERNES)).thenReturn(List.of());

        Disponibilidad disponibilidad = useCase.consultarDisponibilidad(1L, VIERNES);

        assertTrue(disponibilidad.festivo());
        assertFalse(disponibilidad.laboratorioHabilitado());
        assertTrue(disponibilidad.franjas().stream().noneMatch(franja -> franja.disponible()));
    }

    @Test
    void disponibilidadEnDomingoOEnMantenimientoNoHabilita() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));
        when(laboratorioGateway.buscarPorId(2L))
                .thenReturn(Optional.of(laboratorio(2L, EstadoLaboratorio.MANTENIMIENTO)));

        assertFalse(useCase.consultarDisponibilidad(1L, DOMINGO).laboratorioHabilitado());
        assertFalse(useCase.consultarDisponibilidad(2L, VIERNES).laboratorioHabilitado());
    }

    @Test
    void disponibilidadSigueFuncionandoSiLaApiDeFestivosFalla() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));
        when(festivoGateway.esFestivo(VIERNES)).thenThrow(new ServicioExternoException("caída", null));

        Disponibilidad disponibilidad = useCase.consultarDisponibilidad(1L, VIERNES);

        assertFalse(disponibilidad.festivo());
        assertTrue(disponibilidad.laboratorioHabilitado());
    }

    @Test
    void disponibilidadRechazaFechaNula() {
        when(laboratorioGateway.buscarPorId(1L))
                .thenReturn(Optional.of(laboratorio(1L, EstadoLaboratorio.DISPONIBLE)));

        assertThrows(IllegalArgumentException.class, () -> useCase.consultarDisponibilidad(1L, null));
    }

    // ---------------- listarDisponibles ----------------

    @Test
    void listarDisponiblesFiltraMantenimientoYCruces() {
        Laboratorio libre = laboratorio(1L, EstadoLaboratorio.DISPONIBLE);
        Laboratorio ocupado = laboratorio(2L, EstadoLaboratorio.DISPONIBLE);
        Laboratorio enMantenimiento = laboratorio(3L, EstadoLaboratorio.MANTENIMIENTO);
        when(laboratorioGateway.listar()).thenReturn(List.of(libre, ocupado, enMantenimiento));
        when(reservaGateway.existeCruce(1L, VIERNES, LocalTime.of(8, 0), LocalTime.of(10, 0))).thenReturn(false);
        when(reservaGateway.existeCruce(2L, VIERNES, LocalTime.of(8, 0), LocalTime.of(10, 0))).thenReturn(true);

        List<Laboratorio> disponibles = useCase.listarDisponibles(VIERNES, LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertEquals(List.of(libre), disponibles);
    }

    @Test
    void listarDisponiblesEnDomingoOFestivoDevuelveVacio() {
        when(festivoGateway.esFestivo(VIERNES)).thenReturn(true);

        assertTrue(useCase.listarDisponibles(DOMINGO, LocalTime.of(8, 0), LocalTime.of(10, 0)).isEmpty());
        assertTrue(useCase.listarDisponibles(VIERNES, LocalTime.of(8, 0), LocalTime.of(10, 0)).isEmpty());
        verify(laboratorioGateway, never()).listar();
    }

    @Test
    void listarDisponiblesRechazaDatosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.listarDisponibles(null, LocalTime.of(8, 0), LocalTime.of(10, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.listarDisponibles(VIERNES, LocalTime.of(10, 0), LocalTime.of(8, 0)));
    }
}
