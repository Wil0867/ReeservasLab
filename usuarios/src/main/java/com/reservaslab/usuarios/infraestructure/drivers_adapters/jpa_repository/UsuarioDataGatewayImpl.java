package com.reservaslab.usuarios.infraestructure.drivers_adapters.jpa_repository;

import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.domain.model.gateway.UsuarioGateway;
import com.reservaslab.usuarios.infraestructure.mapper.MapperUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioDataGatewayImpl implements UsuarioGateway {

    private final UsuarioDataJpaRepository repository;
    private final MapperUsuario mapperUsuario;

    @Override
    public Usuario guardar(Usuario usuario) {
        return mapperUsuario.toUsuario(repository.save(mapperUsuario.toUsuarioData(usuario)));
    }

    @Override
    public Optional<Usuario> buscarPorId(Long idUsuario) {
        return repository.findById(idUsuario).map(mapperUsuario::toUsuario);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return repository.findByCorreo(correo).map(mapperUsuario::toUsuario);
    }

    @Override
    public List<Usuario> listar() {
        return repository.findAll().stream().map(mapperUsuario::toUsuario).toList();
    }

    @Override
    public void eliminar(Long idUsuario) {
        repository.deleteById(idUsuario);
    }
}
