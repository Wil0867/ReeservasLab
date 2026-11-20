package com.reservaslab.reservas.domain.model.gateway;

import com.reservaslab.reservas.domain.model.Laboratorio;

import java.util.List;
import java.util.Optional;

public interface LaboratorioGateway {
    Laboratorio guardar(Laboratorio laboratorio);

    Optional<Laboratorio> buscarPorId(Long idLaboratorio);

    List<Laboratorio> listar();

    void eliminar(Long idLaboratorio);
}
