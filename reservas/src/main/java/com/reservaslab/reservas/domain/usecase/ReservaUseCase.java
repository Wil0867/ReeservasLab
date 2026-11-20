package com.reservaslab.reservas.domain.usecase;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.EstadoLaboratorio;
import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.ReglasReserva;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.domain.model.UsuarioInfo;
import com.reservaslab.reservas.domain.model.gateway.FestivoGateway;
import com.reservaslab.reservas.domain.model.gateway.LaboratorioGateway;
import com.reservaslab.reservas.domain.model.gateway.NotificacionGateway;
import com.reservaslab.reservas.domain.model.gateway.ReservaGateway;
import com.reservaslab.reservas.domain.model.gateway.UsuarioGateway;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReservaUseCase {

    private final ReservaGateway reservaGateway;
    private final LaboratorioGateway laboratorioGateway;
    private final UsuarioGateway usuarioGateway;
    private final FestivoGateway festivoGateway;
    private final NotificacionGateway notificacionGateway;
    private final Clock clock;      // Reloj inyectado: permite probar con una fecha fija

    // ---------------------------------------------------------------
    // Crear y consultar
    // ---------------------------------------------------------------

    public Reserva crear(Reserva reserva) {
        Reserva nueva = Optional.ofNullable(reserva)
                .orElseThrow(() -> new IllegalArgumentException("Los datos de la reserva son obligatorios"));

        // 1. Datos bien formados (400)
        Long idUsuario = validarId(nueva.getIdUsuario(), "usuario");
        Long idLaboratorio = validarId(nueva.getIdLaboratorio(), "laboratorio");
        LocalDate fecha = Optional.ofNullable(nueva.getFecha())
                .orElseThrow(() -> new IllegalArgumentException("La fecha es obligatoria (formato AAAA-MM-DD)"));
        ReglasReserva.validarFranja(nueva.getHoraInicio(), nueva.getHoraFin());
        Integer personas = Optional.ofNullable(nueva.getCantidadPersonas())
                .filter(cantidad -> cantidad > 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cantidad de personas es obligatoria y debe ser mayor a cero"));
        String motivo = Optional.ofNullable(nueva.getMotivo())
                .map(String::trim)
                .filter(texto -> !texto.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("El motivo de la reserva es obligatorio"));
        if (!LocalDateTime.of(fecha, nueva.getHoraInicio()).isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("La reserva debe ser para una fecha y hora futuras");
        }

        // 2. Reglas del negocio (404 / 409)
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ReglaDeNegocioException("Los laboratorios no abren los domingos");
        }

        UsuarioInfo usuario = usuarioGateway.consultar(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el id: " + idUsuario));
        if (!usuario.activo()) {
            throw new ReglaDeNegocioException("La cuenta del usuario está desactivada y no puede reservar");
        }

        Laboratorio laboratorio = laboratorioGateway.buscarPorId(idLaboratorio)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un laboratorio con el id: " + idLaboratorio));
        if (laboratorio.getEstado() != EstadoLaboratorio.DISPONIBLE) {
            throw new ReglaDeNegocioException(
                    "El laboratorio '" + laboratorio.getNombre() + "' está en mantenimiento");
        }
        if (personas > laboratorio.getCapacidad()) {
            throw new ReglaDeNegocioException("El laboratorio '" + laboratorio.getNombre()
                    + "' admite máximo " + laboratorio.getCapacidad() + " personas");
        }

        if (esFestivoSiSePuedeConsultar(fecha)) {
            throw new ReglaDeNegocioException("El " + fecha + " es festivo: los laboratorios no abren");
        }

        // 3. Guardar solo si el horario sigue libre (verificación y guardado atómicos)
        nueva.setIdReserva(null);
        nueva.setMotivo(motivo);
        nueva.setNombreLaboratorio(laboratorio.getNombre());
        nueva.setEstado(EstadoReserva.PENDIENTE);

        Reserva creada = reservaGateway.crearSiNoHayCruce(nueva)
                .orElseThrow(() -> new ReglaDeNegocioException("El laboratorio '" + laboratorio.getNombre()
                        + "' ya está reservado en ese horario"));

        // 4. Avisar (si falla el aviso, la reserva sigue siendo válida)
        notificar(creada, "Reserva registrada");
        return creada;
    }

    public Reserva buscar(Long idReserva) {
        Long idValidado = validarId(idReserva, "reserva");
        return reservaGateway.buscarPorId(idValidado)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una reserva con el id: " + idValidado));
    }

    /** Historial de reservas de un usuario, de la más reciente a la más antigua. */
    public List<Reserva> listarPorUsuario(Long idUsuario) {
        return reservaGateway.listarPorUsuario(validarId(idUsuario, "usuario"));
    }

    // ---------------------------------------------------------------
    // Ciclo de vida de la reserva
    // ---------------------------------------------------------------

    public Reserva confirmar(Long idReserva) {
        return cambiarEstado(idReserva, EstadoReserva.CONFIRMADA,
                List.of(EstadoReserva.PENDIENTE), "Reserva confirmada");
    }

    public Reserva cancelar(Long idReserva) {
        return cambiarEstado(idReserva, EstadoReserva.CANCELADA,
                List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA), "Reserva cancelada");
    }

    public Reserva finalizar(Long idReserva) {
        return cambiarEstado(idReserva, EstadoReserva.FINALIZADA,
                List.of(EstadoReserva.CONFIRMADA), "Reserva finalizada");
    }

    /** Consulta directa a la API de festivos. Si no responde, se propaga el error (503). */
    public boolean consultarFestivo(LocalDate fecha) {
        LocalDate dia = Optional.ofNullable(fecha)
                .orElseThrow(() -> new IllegalArgumentException("La fecha es obligatoria (formato AAAA-MM-DD)"));
        return festivoGateway.esFestivo(dia);
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    private Reserva cambiarEstado(Long idReserva, EstadoReserva nuevoEstado,
                                  List<EstadoReserva> estadosPermitidos, String asunto) {
        Reserva reserva = buscar(idReserva);
        if (!estadosPermitidos.contains(reserva.getEstado())) {
            throw new ReglaDeNegocioException(
                    "Una reserva en estado " + reserva.getEstado() + " no puede pasar a " + nuevoEstado);
        }
        Reserva actualizada = reservaGateway.actualizarEstado(reserva.getIdReserva(), nuevoEstado);
        notificar(actualizada, asunto);
        return actualizada;
    }

    private Long validarId(Long id, String entidad) {
        return Optional.ofNullable(id)
                .filter(valor -> valor > 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El id de " + entidad + " es obligatorio y debe ser mayor a cero"));
    }

    /** Si la API de festivos no responde, no se bloquean las reservas: se asume día hábil. */
    private boolean esFestivoSiSePuedeConsultar(LocalDate fecha) {
        try {
            return festivoGateway.esFestivo(fecha);
        } catch (ServicioExternoException e) {
            return false;
        }
    }

    /** Mejor esfuerzo: un fallo en Notificaciones nunca daña una reserva ya guardada. */
    private void notificar(Reserva reserva, String asunto) {
        String mensaje = "Laboratorio: " + reserva.getNombreLaboratorio()
                + "\nFecha: " + reserva.getFecha()
                + "\nHorario: " + reserva.getHoraInicio() + " a " + reserva.getHoraFin()
                + "\nEstado: " + reserva.getEstado();
        try {
            notificacionGateway.notificar(reserva.getIdUsuario(), asunto + " #" + reserva.getIdReserva(), mensaje);
        } catch (RuntimeException e) {
            // Sin acción: el aviso es secundario
        }
    }
}
