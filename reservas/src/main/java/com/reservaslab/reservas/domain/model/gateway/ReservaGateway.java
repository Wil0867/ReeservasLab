package com.reservaslab.reservas.domain.model.gateway;

import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.Reserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReservaGateway {

    /**
     * Revisa el cruce de horario y guarda en una sola operación atómica.
     * Devuelve vacío si el laboratorio ya está ocupado en ese horario.
     */
    Optional<Reserva> crearSiNoHayCruce(Reserva reserva);

    Optional<Reserva> buscarPorId(Long idReserva);

    List<Reserva> listarPorUsuario(Long idUsuario);

    /** Reservas PENDIENTES o CONFIRMADAS de un laboratorio en un día, ordenadas por hora. */
    List<Reserva> listarActivasPorLaboratorioYFecha(Long idLaboratorio, LocalDate fecha);

    boolean existeCruce(Long idLaboratorio, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin);

    /** Ids de los laboratorios con reservas activas que se cruzan con el horario (una sola consulta). */
    Set<Long> idsLaboratoriosOcupados(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin);

    boolean existenActivasPorLaboratorio(Long idLaboratorio);

    Reserva actualizarEstado(Long idReserva, EstadoReserva nuevoEstado);
}
