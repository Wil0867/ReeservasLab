package com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio;

import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.model.gateway.LaboratorioGateway;
import com.reservaslab.reservas.infraestructure.mapper.MapperLaboratorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LaboratorioDataGatewayImpl implements LaboratorioGateway {

    private final LaboratorioDataJpaRepository repository;
    private final MapperLaboratorio mapperLaboratorio;

    @Override
    public Laboratorio guardar(Laboratorio laboratorio) {
        return mapperLaboratorio.toLaboratorio(repository.save(mapperLaboratorio.toLaboratorioData(laboratorio)));
    }

    @Override
    public Optional<Laboratorio> buscarPorId(Long idLaboratorio) {
        return repository.findById(idLaboratorio).map(mapperLaboratorio::toLaboratorio);
    }

    @Override
    public List<Laboratorio> listar() {
        return repository.findAll().stream().map(mapperLaboratorio::toLaboratorio).toList();
    }

    @Override
    public void eliminar(Long idLaboratorio) {
        repository.deleteById(idLaboratorio);
    }
}
