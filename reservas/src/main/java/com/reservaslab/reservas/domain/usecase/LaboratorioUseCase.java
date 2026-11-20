package com.reservaslab.reservas.domain.usecase;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.Disponibilidad;
import com.reservaslab.reservas.domain.model.EstadoLaboratorio;
import com.reservaslab.reservas.domain.model.Franja;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.ReglasReserva;
import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.domain.model.gateway.FestivoGateway;
import com.reservaslab.reservas.domain.model.gateway.LaboratorioGateway;
import com.reservaslab.reservas.domain.model.gateway.ReservaGateway;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class LaboratorioUseCase {

    private final LaboratorioGateway laboratorioGateway;
    private final ReservaGateway reservaGateway;
    private final FestivoGateway festivoGateway;

    // ---------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------

    public Laboratorio crear(Laboratorio laboratorio) {
        Laboratorio nuevo = Optional.ofNullable(laboratorio)
                .orElseThrow(() -> new IllegalArgumentException("Los datos del laboratorio son obligatorios"));
        validarDatos(nuevo);

        nuevo.setIdLaboratorio(null);
        nuevo.setNombre(nuevo.getNombre().trim());
        nuevo.setUbicacion(nuevo.getUbicacion().trim());
        nuevo.setEstado(EstadoLaboratorio.DISPONIBLE);     // todo laboratorio nuevo queda habilitado
        return laboratorioGateway.guardar(nuevo);
    }

    public Laboratorio buscar(Long idLaboratorio) {
        Long idValidado = Optional.ofNullable(idLaboratorio)
                .filter(valor -> valor > 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El id del laboratorio es obligatorio y debe ser mayor a cero"));
        return laboratorioGateway.buscarPorId(idValidado)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un laboratorio con el id: " + idValidado));
    }

    public List<Laboratorio> listar() {
        return laboratorioGateway.listar();
    }

    /** Cambia nombre, ubicación y capacidad. El estado tiene su propia operación. */
    public Laboratorio actualizar(Long idLaboratorio, Laboratorio datos) {
        Laboratorio existente = buscar(idLaboratorio);
        Laboratorio cambios = Optional.ofNullable(datos)
                .orElseThrow(() -> new IllegalArgumentException("Los datos del laboratorio son obligatorios"));
        validarDatos(cambios);

        existente.setNombre(cambios.getNombre().trim());
        existente.setUbicacion(cambios.getUbicacion().trim());
        existente.setCapacidad(cambios.getCapacidad());
        return laboratorioGateway.guardar(existente);
    }

    public void eliminar(Long idLaboratorio) {
        Laboratorio existente = buscar(idLaboratorio);
        if (reservaGateway.existenActivasPorLaboratorio(existente.getIdLaboratorio())) {
            throw new ReglaDeNegocioException(
                    "No se puede eliminar el laboratorio porque tiene reservas pendientes o confirmadas");
        }
        laboratorioGateway.eliminar(existente.getIdLaboratorio());
    }

    // ---------------------------------------------------------------
    // Operaciones de negocio (no CRUD)
    // ---------------------------------------------------------------

    /** Pone el laboratorio en MANTENIMIENTO o lo vuelve a dejar DISPONIBLE. */
    public Laboratorio cambiarEstado(Long idLaboratorio, EstadoLaboratorio nuevoEstado) {
        Laboratorio laboratorio = buscar(idLaboratorio);
        EstadoLaboratorio estado = Optional.ofNullable(nuevoEstado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El estado es obligatorio: DISPONIBLE o MANTENIMIENTO"));
        if (estado == laboratorio.getEstado()) {
            throw new ReglaDeNegocioException("El laboratorio ya está en estado " + estado);
        }
        laboratorio.setEstado(estado);
        return laboratorioGateway.guardar(laboratorio);
    }

    /** Agenda del laboratorio para un día: una franja por hora, libre u ocupada. */
    public Disponibilidad consultarDisponibilidad(Long idLaboratorio, LocalDate fecha) {
        Laboratorio laboratorio = buscar(idLaboratorio);
        LocalDate dia = validarFecha(fecha);

        boolean festivo = esFestivo(dia);
        boolean habilitado = laboratorio.getEstado() == EstadoLaboratorio.DISPONIBLE
                && !festivo
                && dia.getDayOfWeek() != DayOfWeek.SUNDAY;

        List<Reserva> reservas = reservaGateway.listarActivasPorLaboratorioYFecha(laboratorio.getIdLaboratorio(), dia);

        List<Franja> franjas = new ArrayList<>();
        LocalTime inicio = ReglasReserva.APERTURA;
        while (inicio.isBefore(ReglasReserva.CIERRE)) {
            LocalTime desde = inicio;
            LocalTime hasta = inicio.plusHours(1);
            // Dos horarios se cruzan si uno empieza antes de que el otro termine, y viceversa
            boolean ocupada = reservas.stream().anyMatch(reserva ->
                    reserva.getHoraInicio().isBefore(hasta) && reserva.getHoraFin().isAfter(desde));
            franjas.add(new Franja(desde, hasta, habilitado && !ocupada));
            inicio = hasta;
        }
        return new Disponibilidad(laboratorio.getIdLaboratorio(), dia, festivo, habilitado, franjas);
    }

    /** Laboratorios habilitados y sin reservas que se crucen con el horario pedido. */
    public List<Laboratorio> listarDisponibles(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        LocalDate dia = validarFecha(fecha);
        ReglasReserva.validarFranja(horaInicio, horaFin);

        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY || esFestivo(dia)) {
            return List.of();
        }
        return laboratorioGateway.listar().stream()
                .filter(laboratorio -> laboratorio.getEstado() == EstadoLaboratorio.DISPONIBLE)
                .filter(laboratorio -> !reservaGateway.existeCruce(
                        laboratorio.getIdLaboratorio(), dia, horaInicio, horaFin))
                .toList();
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    private void validarDatos(Laboratorio laboratorio) {
        Optional.ofNullable(laboratorio.getNombre())
                .filter(nombre -> !nombre.trim().isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("El nombre del laboratorio es obligatorio"));
        Optional.ofNullable(laboratorio.getUbicacion())
                .filter(ubicacion -> !ubicacion.trim().isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("La ubicación del laboratorio es obligatoria"));
        Optional.ofNullable(laboratorio.getCapacidad())
                .filter(capacidad -> capacidad > 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La capacidad es obligatoria y debe ser mayor a cero"));
    }

    private LocalDate validarFecha(LocalDate fecha) {
        return Optional.ofNullable(fecha)
                .orElseThrow(() -> new IllegalArgumentException("La fecha es obligatoria (formato AAAA-MM-DD)"));
    }

    /** Si la API de festivos no responde, se asume que el día es hábil para no detener el servicio. */
    private boolean esFestivo(LocalDate fecha) {
        try {
            return festivoGateway.esFestivo(fecha);
        } catch (ServicioExternoException e) {
            return false;
        }
    }
}
