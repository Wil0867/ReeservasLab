package com.reservaslab.reservas.infraestructure.mapper;

import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioData;
import com.reservaslab.reservas.infraestructure.entry_points.dto.LaboratorioRequest;
import org.springframework.stereotype.Component;

@Component
public class MapperLaboratorio {

    public Laboratorio toLaboratorio(LaboratorioData data) {
        if (data == null) return null;
        return new Laboratorio(data.getIdLaboratorio(), data.getNombre(), data.getUbicacion(),
                data.getCapacidad(), data.getEstado());
    }

    public LaboratorioData toLaboratorioData(Laboratorio laboratorio) {
        if (laboratorio == null) return null;
        return new LaboratorioData(laboratorio.getIdLaboratorio(), laboratorio.getNombre(),
                laboratorio.getUbicacion(), laboratorio.getCapacidad(), laboratorio.getEstado());
    }

    public Laboratorio toLaboratorio(LaboratorioRequest request) {
        if (request == null) return null;
        return new Laboratorio(null, request.nombre(), request.ubicacion(), request.capacidad(), null);
    }
}
