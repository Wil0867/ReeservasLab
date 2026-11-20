package com.reservaslab.reservas.infraestructure.drivers_adapters;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.model.EstadoLaboratorio;
import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.ReglasReserva;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioData;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioDataGatewayImpl;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioDataJpaRepository;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva.ReservaData;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva.ReservaDataGatewayImpl;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva.ReservaDataJpaRepository;
import com.reservaslab.reservas.infraestructure.mapper.MapperLaboratorio;
import com.reservaslab.reservas.infraestructure.mapper.MapperReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Adaptadores de base de datos, con los repositorios JPA simulados. */
class JpaGatewaysTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DIEZ = LocalTime.of(10, 0);

    private LaboratorioDataJpaRepository laboratorioRepository;
    private ReservaDataJpaRepository reservaRepository;
    private LaboratorioDataGatewayImpl laboratorioGateway;
    private ReservaDataGatewayImpl reservaGateway;

    @BeforeEach
    void preparar() {
        laboratorioRepository = mock(LaboratorioDataJpaRepository.class);
        reservaRepository = mock(ReservaDataJpaRepository.class);
        laboratorioGateway = new LaboratorioDataGatewayImpl(laboratorioRepository, new MapperLaboratorio());
        reservaGateway = new ReservaDataGatewayImpl(reservaRepository, laboratorioRepository, new MapperReserva());
    }

    private LaboratorioData laboratorioData() {
        return new LaboratorioData(2L, "Lab Redes", "Bloque B", 20, EstadoLaboratorio.DISPONIBLE);
    }

    private ReservaData reservaData(EstadoReserva estado) {
        ReservaData data = new ReservaData();
        data.setIdReserva(10L);
        data.setIdUsuario(1L);
        data.setLaboratorio(laboratorioData());
        data.setFecha(FECHA);
        data.setHoraInicio(OCHO);
        data.setHoraFin(DIEZ);
        data.setCantidadPersonas(5);
        data.setMotivo("Práctica");
        data.setEstado(estado);
        return data;
    }

    private Reserva solicitud() {
        return new Reserva(null, 1L, 2L, "Lab Redes", FECHA, OCHO, DIEZ, 5, "Práctica", EstadoReserva.PENDIENTE);
    }

    // ---------------- laboratorios ----------------

    @Test
    void laboratorioGuardar() {
        when(laboratorioRepository.save(any())).thenReturn(laboratorioData());

        Laboratorio guardado = laboratorioGateway.guardar(
                new Laboratorio(null, "Lab Redes", "Bloque B", 20, EstadoLaboratorio.DISPONIBLE));

        assertEquals(2L, guardado.getIdLaboratorio());
    }

    @Test
    void laboratorioBuscarListarYEliminar() {
        when(laboratorioRepository.findById(2L)).thenReturn(Optional.of(laboratorioData()));
        when(laboratorioRepository.findById(9L)).thenReturn(Optional.empty());
        when(laboratorioRepository.findAll()).thenReturn(List.of(laboratorioData()));

        assertEquals("Lab Redes", laboratorioGateway.buscarPorId(2L).orElseThrow().getNombre());
        assertTrue(laboratorioGateway.buscarPorId(9L).isEmpty());
        assertEquals(1, laboratorioGateway.listar().size());

        laboratorioGateway.eliminar(2L);
        verify(laboratorioRepository).deleteById(2L);
    }

    // ---------------- reservas ----------------

    @Test
    void crearSiNoHayCruceGuardaLaReserva() {
        when(laboratorioRepository.bloquearPorId(2L)).thenReturn(Optional.of(laboratorioData()));
        when(reservaRepository.existeCruce(2L, FECHA, ReglasReserva.ESTADOS_ACTIVOS, OCHO, DIEZ)).thenReturn(false);
        when(reservaRepository.save(any())).thenAnswer(invocacion -> {
            ReservaData data = invocacion.getArgument(0);
            data.setIdReserva(10L);
            return data;
        });

        Reserva creada = reservaGateway.crearSiNoHayCruce(solicitud()).orElseThrow();

        assertEquals(10L, creada.getIdReserva());
        assertEquals(2L, creada.getIdLaboratorio());
        assertEquals("Lab Redes", creada.getNombreLaboratorio());
        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
        assertEquals(5, creada.getCantidadPersonas());
    }

    @Test
    void crearSiNoHayCruceDevuelveVacioSiElHorarioEstaOcupado() {
        when(laboratorioRepository.bloquearPorId(2L)).thenReturn(Optional.of(laboratorioData()));
        when(reservaRepository.existeCruce(2L, FECHA, ReglasReserva.ESTADOS_ACTIVOS, OCHO, DIEZ)).thenReturn(true);

        assertTrue(reservaGateway.crearSiNoHayCruce(solicitud()).isEmpty());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearSiNoHayCruceFallaSiElLaboratorioNoExiste() {
        when(laboratorioRepository.bloquearPorId(2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> reservaGateway.crearSiNoHayCruce(solicitud()));
    }

    @Test
    void consultasDeReservas() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaData(EstadoReserva.PENDIENTE)));
        when(reservaRepository.findByIdUsuarioOrderByFechaDescHoraInicioDesc(1L))
                .thenReturn(List.of(reservaData(EstadoReserva.CANCELADA)));
        when(reservaRepository.findByLaboratorio_IdLaboratorioAndFechaAndEstadoInOrderByHoraInicioAsc(
                2L, FECHA, ReglasReserva.ESTADOS_ACTIVOS))
                .thenReturn(List.of(reservaData(EstadoReserva.CONFIRMADA)));
        when(reservaRepository.existeCruce(2L, FECHA, ReglasReserva.ESTADOS_ACTIVOS, OCHO, DIEZ)).thenReturn(true);
        when(reservaRepository.existsByLaboratorio_IdLaboratorioAndEstadoIn(2L, ReglasReserva.ESTADOS_ACTIVOS))
                .thenReturn(true);

        assertEquals(10L, reservaGateway.buscarPorId(10L).orElseThrow().getIdReserva());
        assertEquals(EstadoReserva.CANCELADA, reservaGateway.listarPorUsuario(1L).get(0).getEstado());
        assertEquals(1, reservaGateway.listarActivasPorLaboratorioYFecha(2L, FECHA).size());
        assertTrue(reservaGateway.existeCruce(2L, FECHA, OCHO, DIEZ));
        assertTrue(reservaGateway.existenActivasPorLaboratorio(2L));
    }

    @Test
    void idsLaboratoriosOcupadosUsaUnaSolaConsulta() {
        when(reservaRepository.idsLaboratoriosConCruce(FECHA, ReglasReserva.ESTADOS_ACTIVOS, OCHO, DIEZ))
                .thenReturn(List.of(2L, 5L));

        assertEquals(java.util.Set.of(2L, 5L), reservaGateway.idsLaboratoriosOcupados(FECHA, OCHO, DIEZ));
        verify(reservaRepository, times(1)).idsLaboratoriosConCruce(FECHA, ReglasReserva.ESTADOS_ACTIVOS, OCHO, DIEZ);
    }

    @Test
    void actualizarEstadoCambiaYGuarda() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaData(EstadoReserva.PENDIENTE)));
        when(reservaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertEquals(EstadoReserva.CONFIRMADA,
                reservaGateway.actualizarEstado(10L, EstadoReserva.CONFIRMADA).getEstado());
    }

    @Test
    void actualizarEstadoFallaSiLaReservaNoExiste() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> reservaGateway.actualizarEstado(10L, EstadoReserva.CONFIRMADA));
    }
}
