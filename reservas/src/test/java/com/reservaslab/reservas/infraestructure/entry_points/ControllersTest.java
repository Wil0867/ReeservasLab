package com.reservaslab.reservas.infraestructure.entry_points;

import com.reservaslab.reservas.domain.model.Disponibilidad;
import com.reservaslab.reservas.domain.model.EstadoLaboratorio;
import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.domain.usecase.LaboratorioUseCase;
import com.reservaslab.reservas.domain.usecase.ReservaUseCase;
import com.reservaslab.reservas.infraestructure.entry_points.dto.EstadoLaboratorioRequest;
import com.reservaslab.reservas.infraestructure.entry_points.dto.LaboratorioRequest;
import com.reservaslab.reservas.infraestructure.entry_points.dto.ReservaRequest;
import com.reservaslab.reservas.infraestructure.mapper.MapperLaboratorio;
import com.reservaslab.reservas.infraestructure.mapper.MapperReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Controladores llamados como objetos normales, con los casos de uso simulados. */
class ControllersTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DIEZ = LocalTime.of(10, 0);

    private LaboratorioUseCase laboratorioUseCase;
    private ReservaUseCase reservaUseCase;
    private LaboratorioController laboratorioController;
    private ReservaController reservaController;

    private final Laboratorio laboratorio =
            new Laboratorio(2L, "Lab Redes", "Bloque B", 20, EstadoLaboratorio.DISPONIBLE);
    private final Reserva reserva =
            new Reserva(10L, 1L, 2L, "Lab Redes", FECHA, OCHO, DIEZ, 5, "Práctica", EstadoReserva.PENDIENTE);

    @BeforeEach
    void preparar() {
        laboratorioUseCase = mock(LaboratorioUseCase.class);
        reservaUseCase = mock(ReservaUseCase.class);
        laboratorioController = new LaboratorioController(laboratorioUseCase, new MapperLaboratorio());
        reservaController = new ReservaController(reservaUseCase, new MapperReserva());
    }

    // ---------------- laboratorios ----------------

    @Test
    void crearLaboratorioResponde201() {
        when(laboratorioUseCase.crear(any())).thenReturn(laboratorio);

        ResponseEntity<Laboratorio> respuesta =
                laboratorioController.crear(new LaboratorioRequest("Lab Redes", "Bloque B", 20));

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertSame(laboratorio, respuesta.getBody());
    }

    @Test
    void listarBuscarYActualizarLaboratorio() {
        when(laboratorioUseCase.listar()).thenReturn(List.of(laboratorio));
        when(laboratorioUseCase.buscar(2L)).thenReturn(laboratorio);
        when(laboratorioUseCase.actualizar(eq(2L), any())).thenReturn(laboratorio);

        assertEquals(List.of(laboratorio), laboratorioController.listar().getBody());
        assertSame(laboratorio, laboratorioController.buscar(2L).getBody());
        ResponseEntity<Laboratorio> actualizado =
                laboratorioController.actualizar(2L, new LaboratorioRequest("Lab Redes", "Bloque B", 20));
        assertEquals(HttpStatus.OK, actualizado.getStatusCode());
        assertSame(laboratorio, actualizado.getBody());
    }

    @Test
    void eliminarLaboratorioRespondeMensaje() {
        ResponseEntity<Map<String, Object>> respuesta = laboratorioController.eliminar(2L);

        verify(laboratorioUseCase).eliminar(2L);
        assertEquals("Laboratorio eliminado correctamente", respuesta.getBody().get("mensaje"));
    }

    @Test
    void cambiarEstadoDisponibilidadYDisponibles() {
        Disponibilidad disponibilidad = new Disponibilidad(2L, FECHA, false, true, List.of());
        when(laboratorioUseCase.cambiarEstado(2L, EstadoLaboratorio.MANTENIMIENTO)).thenReturn(laboratorio);
        when(laboratorioUseCase.consultarDisponibilidad(2L, FECHA)).thenReturn(disponibilidad);
        when(laboratorioUseCase.listarDisponibles(FECHA, OCHO, DIEZ)).thenReturn(List.of(laboratorio));

        assertSame(laboratorio, laboratorioController
                .cambiarEstado(2L, new EstadoLaboratorioRequest(EstadoLaboratorio.MANTENIMIENTO)).getBody());
        assertSame(disponibilidad, laboratorioController.disponibilidad(2L, FECHA).getBody());
        assertEquals(List.of(laboratorio), laboratorioController.disponibles(FECHA, OCHO, DIEZ).getBody());
    }

    // ---------------- reservas ----------------

    @Test
    void crearReservaResponde201() {
        when(reservaUseCase.crear(any())).thenReturn(reserva);

        ResponseEntity<Reserva> respuesta =
                reservaController.crear(new ReservaRequest(1L, 2L, FECHA, OCHO, DIEZ, 5, "Práctica"));

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertSame(reserva, respuesta.getBody());
    }

    @Test
    void buscarEHistorial() {
        when(reservaUseCase.buscar(10L)).thenReturn(reserva);
        when(reservaUseCase.listarPorUsuario(1L)).thenReturn(List.of(reserva));

        assertSame(reserva, reservaController.buscar(10L).getBody());
        assertEquals(List.of(reserva), reservaController.historial(1L).getBody());
    }

    @Test
    void confirmarCancelarYFinalizar() {
        when(reservaUseCase.confirmar(10L)).thenReturn(reserva);
        when(reservaUseCase.cancelar(10L)).thenReturn(reserva);
        when(reservaUseCase.finalizar(10L)).thenReturn(reserva);

        assertSame(reserva, reservaController.confirmar(10L).getBody());
        assertSame(reserva, reservaController.cancelar(10L).getBody());
        assertSame(reserva, reservaController.finalizar(10L).getBody());
    }

    @Test
    void festivoRespondeFechaYResultado() {
        LocalDate navidad = LocalDate.of(2026, 12, 25);
        when(reservaUseCase.consultarFestivo(navidad)).thenReturn(true);

        Map<String, Object> cuerpo = reservaController.festivo(navidad).getBody();

        assertEquals("2026-12-25", cuerpo.get("fecha"));
        assertEquals(true, cuerpo.get("festivo"));
    }
}
