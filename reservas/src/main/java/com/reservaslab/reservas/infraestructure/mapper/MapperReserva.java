package com.reservaslab.reservas.infraestructure.mapper;

import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva.ReservaData;
import com.reservaslab.reservas.infraestructure.entry_points.dto.ReservaRequest;
import org.springframework.stereotype.Component;

/**
 * De dominio a Data lo hace el adaptador, porque necesita el laboratorio ya cargado de la base.
 */
@Component
public class MapperReserva {

    public Reserva toReserva(ReservaData data) {
        if (data == null) return null;
        return new Reserva(
                data.getIdReserva(),
                data.getIdUsuario(),
                data.getLaboratorio().getIdLaboratorio(),
                data.getLaboratorio().getNombre(),
                data.getFecha(),
                data.getHoraInicio(),
                data.getHoraFin(),
                data.getCantidadPersonas(),
                data.getMotivo(),
                data.getEstado());
    }

    /** El cliente no decide el id ni el estado. */
    public Reserva toReserva(ReservaRequest request) {
        if (request == null) return null;
        return new Reserva(null, request.idUsuario(), request.idLaboratorio(), null, request.fecha(),
                request.horaInicio(), request.horaFin(), request.cantidadPersonas(), request.motivo(), null);
    }
}
