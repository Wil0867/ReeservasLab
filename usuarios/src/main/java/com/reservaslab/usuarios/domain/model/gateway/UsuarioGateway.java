package com.reservaslab.usuarios.domain.model.gateway;

import com.reservaslab.usuarios.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioGateway {
    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long idUsuario);

    Optional<Usuario> buscarPorCorreo(String correo);

    List<Usuario> listar();

    void eliminar(Long idUsuario);
}
