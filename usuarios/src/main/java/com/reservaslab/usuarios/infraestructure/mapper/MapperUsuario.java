package com.reservaslab.usuarios.infraestructure.mapper;

import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.infraestructure.drivers_adapters.jpa_repository.UsuarioData;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.ActualizarRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.RegistroRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.UsuarioResponse;
import org.springframework.stereotype.Component;

@Component
public class MapperUsuario {

    public Usuario toUsuario(UsuarioData data) {
        if (data == null) return null;
        return new Usuario(data.getIdUsuario(), data.getNombre(), data.getCorreo(),
                data.getClave(), data.getRol(), data.getActivo());
    }

    public UsuarioData toUsuarioData(Usuario usuario) {
        if (usuario == null) return null;
        return new UsuarioData(usuario.getIdUsuario(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getClave(), usuario.getRol(), usuario.getActivo());
    }

    public Usuario toUsuario(RegistroRequest request) {
        if (request == null) return null;
        return new Usuario(null, request.nombre(), request.correo(), request.clave(), request.rol(), null);
    }

    public Usuario toUsuario(ActualizarRequest request) {
        if (request == null) return null;
        return new Usuario(null, request.nombre(), request.correo(), null, request.rol(), null);
    }

    /** Hacia afuera nunca viaja la clave. */
    public UsuarioResponse toResponse(Usuario usuario) {
        if (usuario == null) return null;
        return new UsuarioResponse(usuario.getIdUsuario(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), usuario.getActivo());
    }
}
