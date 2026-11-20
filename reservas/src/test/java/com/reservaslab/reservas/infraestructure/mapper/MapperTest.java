package com.reservaslab.reservas.infraestructure.mapper;

import com.reservaslab.reservas.domain.model.EstadoLaboratorio;
import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioData;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva.ReservaData;
import com.reservaslab.reservas.infraestructure.entry_points.dto.LaboratorioRequest;
import com.reservaslab.reservas.infraestructure.entry_points.dto.ReservaRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapperTest {

    private final MapperLaboratorio mapperLaboratorio = new MapperLaboratorio();
    private final MapperReserva mapperReserva = new MapperReserva();

    @Test
    void laboratorioDeDataADominioYAlReves() {
        LaboratorioData data = new LaboratorioData(1L, "Lab Redes", "Bloque B", 20, EstadoLaboratorio.DISPONIBLE);

        Laboratorio laboratorio = mapperLaboratorio.toLaboratorio(data);
        assertEquals(1L, laboratorio.getIdLaboratorio());
        assertEquals("Lab Redes", laboratorio.getNombre());
        assertEquals("Bloque B", laboratorio.getUbicacion());
        assertEquals(20, laboratorio.getCapacidad());
        assertEquals(EstadoLaboratorio.DISPONIBLE, laboratorio.getEstado());

        LaboratorioData deVuelta = mapperLaboratorio.toLaboratorioData(laboratorio);
        assertEquals(1L, deVuelta.getIdLaboratorio());
        assertEquals("Lab Redes", deVuelta.getNombre());
        assertEquals(EstadoLaboratorio.DISPONIBLE, deVuelta.getEstado());
    }

    @Test
    void laboratorioDesdeElRequestNoTraeIdNiEstado() {
        Laboratorio laboratorio = mapperLaboratorio.toLaboratorio(new LaboratorioRequest("Lab IoT", "Bloque C", 15));

        assertNull(laboratorio.getIdLaboratorio());
        assertNull(laboratorio.getEstado());
        assertEquals("Lab IoT", laboratorio.getNombre());
        assertEquals(15, laboratorio.getCapacidad());
    }

    @Test
    void reservaDeDataADominioTomaElNombreDelLaboratorio() {
        ReservaData data = new ReservaData();
        data.setIdReserva(10L);
        data.setIdUsuario(1L);
        data.setLaboratorio(new LaboratorioData(2L, "Lab Redes", "Bloque B", 20, EstadoLaboratorio.DISPONIBLE));
        data.setFecha(LocalDate.of(2026, 11, 20));
        data.setHoraInicio(LocalTime.of(8, 0));
        data.setHoraFin(LocalTime.of(10, 0));
        data.setCantidadPersonas(5);
        data.setMotivo("Práctica");
        data.setEstado(EstadoReserva.PENDIENTE);

        Reserva reserva = mapperReserva.toReserva(data);

        assertEquals(10L, reserva.getIdReserva());
        assertEquals(1L, reserva.getIdUsuario());
        assertEquals(2L, reserva.getIdLaboratorio());
        assertEquals("Lab Redes", reserva.getNombreLaboratorio());
        assertEquals(LocalDate.of(2026, 11, 20), reserva.getFecha());
        assertEquals(LocalTime.of(8, 0), reserva.getHoraInicio());
        assertEquals(LocalTime.of(10, 0), reserva.getHoraFin());
        assertEquals(5, reserva.getCantidadPersonas());
        assertEquals("Práctica", reserva.getMotivo());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    void reservaDesdeElRequestNoTraeIdNiEstado() {
        Reserva reserva = mapperReserva.toReserva(new ReservaRequest(1L, 2L, LocalDate.of(2026, 11, 20),
                LocalTime.of(8, 0), LocalTime.of(10, 0), 5, "Práctica"));

        assertNull(reserva.getIdReserva());
        assertNull(reserva.getEstado());
        assertEquals(1L, reserva.getIdUsuario());
        assertEquals(2L, reserva.getIdLaboratorio());
        assertEquals("Práctica", reserva.getMotivo());
    }

    @Test
    void losNulosSeDevuelvenComoNulos() {
        assertNull(mapperLaboratorio.toLaboratorio((LaboratorioData) null));
        assertNull(mapperLaboratorio.toLaboratorio((LaboratorioRequest) null));
        assertNull(mapperLaboratorio.toLaboratorioData(null));
        assertNull(mapperReserva.toReserva((ReservaData) null));
        assertNull(mapperReserva.toReserva((ReservaRequest) null));
    }
}
