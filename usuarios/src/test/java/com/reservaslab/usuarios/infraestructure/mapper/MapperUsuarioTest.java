package com.reservaslab.usuarios.infraestructure.mapper;

import com.reservaslab.usuarios.domain.model.Rol;
import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.infraestructure.drivers_adapters.jpa_repository.UsuarioData;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.ActualizarRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.RegistroRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapperUsuarioTest {

    private final MapperUsuario mapper = new MapperUsuario();

    @Test
    void convierteDeDataADominioYAlReves() {
        UsuarioData data = new UsuarioData(1L, "Ana", "ana@mail.com", "hash", Rol.DOCENTE, true);

        Usuario usuario = mapper.toUsuario(data);
        assertEquals(1L, usuario.getIdUsuario());
        assertEquals("Ana", usuario.getNombre());
        assertEquals("ana@mail.com", usuario.getCorreo());
        assertEquals("hash", usuario.getClave());
        assertEquals(Rol.DOCENTE, usuario.getRol());
        assertTrue(usuario.getActivo());

        UsuarioData deVuelta = mapper.toUsuarioData(usuario);
        assertEquals(1L, deVuelta.getIdUsuario());
        assertEquals("ana@mail.com", deVuelta.getCorreo());
        assertEquals("hash", deVuelta.getClave());
        assertEquals(Rol.DOCENTE, deVuelta.getRol());
    }

    @Test
    void registroNoTraeIdNiEstado() {
        Usuario usuario = mapper.toUsuario(new RegistroRequest("Ana", "ana@mail.com", "secreta", Rol.ESTUDIANTE));

        assertNull(usuario.getIdUsuario());
        assertNull(usuario.getActivo());
        assertEquals("secreta", usuario.getClave());
    }

    @Test
    void actualizacionNoTraeClave() {
        Usuario usuario = mapper.toUsuario(new ActualizarRequest("Ana", "ana@mail.com", Rol.ADMIN));

        assertNull(usuario.getClave());
        assertEquals(Rol.ADMIN, usuario.getRol());
    }

    @Test
    void laRespuestaNoIncluyeLaClave() {
        UsuarioResponse respuesta = mapper.toResponse(new Usuario(1L, "Ana", "ana@mail.com", "hash", Rol.ESTUDIANTE, true));

        assertEquals(new UsuarioResponse(1L, "Ana", "ana@mail.com", Rol.ESTUDIANTE, true), respuesta);
    }

    @Test
    void losNulosNoRompen() {
        assertNull(mapper.toUsuario((UsuarioData) null));
        assertNull(mapper.toUsuarioData(null));
        assertNull(mapper.toUsuario((RegistroRequest) null));
        assertNull(mapper.toUsuario((ActualizarRequest) null));
        assertNull(mapper.toResponse(null));
    }
}
