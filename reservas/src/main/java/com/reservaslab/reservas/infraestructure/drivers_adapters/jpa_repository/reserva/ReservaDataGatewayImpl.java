package com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.ReglasReserva;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.domain.model.gateway.ReservaGateway;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioData;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioDataJpaRepository;
import com.reservaslab.reservas.infraestructure.mapper.MapperReserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ReservaDataGatewayImpl implements ReservaGateway {

    private final ReservaDataJpaRepository reservaRepository;
    private final LaboratorioDataJpaRepository laboratorioRepository;
    private final MapperReserva mapperReserva;

    /**
     * Todo dentro de una transacción: se bloquea el laboratorio, se revisa el cruce y se guarda.
     * Así dos personas no pueden reservar el mismo laboratorio a la misma hora al mismo tiempo.
     */
    @Override
    @Transactional
    public Optional<Reserva> crearSiNoHayCruce(Reserva reserva) {
        LaboratorioData laboratorio = laboratorioRepository.bloquearPorId(reserva.getIdLaboratorio())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un laboratorio con el id: " + reserva.getIdLaboratorio()));

        if (reservaRepository.existeCruce(laboratorio.getIdLaboratorio(), reserva.getFecha(),
                ReglasReserva.ESTADOS_ACTIVOS, reserva.getHoraInicio(), reserva.getHoraFin())) {
            return Optional.empty();
        }

        ReservaData data = new ReservaData();
        data.setIdUsuario(reserva.getIdUsuario());
        data.setLaboratorio(laboratorio);
        data.setFecha(reserva.getFecha());
        data.setHoraInicio(reserva.getHoraInicio());
        data.setHoraFin(reserva.getHoraFin());
        data.setCantidadPersonas(reserva.getCantidadPersonas());
        data.setMotivo(reserva.getMotivo());
        data.setEstado(reserva.getEstado());
        return Optional.of(mapperReserva.toReserva(reservaRepository.save(data)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorId(Long idReserva) {
        return reservaRepository.findById(idReserva).map(mapperReserva::toReserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarPorUsuario(Long idUsuario) {
        return reservaRepository.findByIdUsuarioOrderByFechaDescHoraInicioDesc(idUsuario).stream()
                .map(mapperReserva::toReserva)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarActivasPorLaboratorioYFecha(Long idLaboratorio, LocalDate fecha) {
        return reservaRepository
                .findByLaboratorio_IdLaboratorioAndFechaAndEstadoInOrderByHoraInicioAsc(
                        idLaboratorio, fecha, ReglasReserva.ESTADOS_ACTIVOS)
                .stream()
                .map(mapperReserva::toReserva)
                .toList();
    }

    @Override
    public boolean existeCruce(Long idLaboratorio, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        return reservaRepository.existeCruce(idLaboratorio, fecha, ReglasReserva.ESTADOS_ACTIVOS, horaInicio, horaFin);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> idsLaboratoriosOcupados(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        return Set.copyOf(reservaRepository.idsLaboratoriosConCruce(
                fecha, ReglasReserva.ESTADOS_ACTIVOS, horaInicio, horaFin));
    }

    @Override
    public boolean existenActivasPorLaboratorio(Long idLaboratorio) {
        return reservaRepository.existsByLaboratorio_IdLaboratorioAndEstadoIn(
                idLaboratorio, ReglasReserva.ESTADOS_ACTIVOS);
    }

    @Override
    @Transactional
    public Reserva actualizarEstado(Long idReserva, EstadoReserva nuevoEstado) {
        ReservaData data = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una reserva con el id: " + idReserva));
        data.setEstado(nuevoEstado);
        return mapperReserva.toReserva(reservaRepository.save(data));
    }
}
