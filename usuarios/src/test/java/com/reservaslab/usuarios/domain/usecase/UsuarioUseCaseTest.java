package com.reservaslab.usuarios.domain.usecase;

import com.reservaslab.usuarios.domain.Excepciones.AccesoDenegadoException;
import com.reservaslab.usuarios.domain.Excepciones.ConflictoException;
import com.reservaslab.usuarios.domain.Excepciones.CredencialesInvalidasException;
import com.reservaslab.usuarios.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.usuarios.domain.model.EstadisticasUsuarios;
import com.reservaslab.usuarios.domain.model.Rol;
import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.domain.model.gateway.EncriptarGateway;
import com.reservaslab.usuarios.domain.model.gateway.UsuarioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UsuarioUseCaseTest {

    private UsuarioGateway usuarioGateway;
    private EncriptarGateway encriptarGateway;
    private UsuarioUseCase useCase;

    @BeforeEach
    void preparar() {
        usuarioGateway = mock(UsuarioGateway.class);
        encriptarGateway = mock(EncriptarGateway.class);
        useCase = new UsuarioUseCase(usuarioGateway, encriptarGateway);

        // guardar devuelve lo mismo que recibe; encriptar antepone "hash-"
        when(usuarioGateway.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encriptarGateway.encriptar(anyString())).thenAnswer(inv -> "hash-" + inv.getArgument(0));
        when(encriptarGateway.verificar("secreta", "hash-secreta")).thenReturn(true);
    }

    private Usuario guardado(Long id, String correo, Boolean activo) {
        return new Usuario(id, "Ana", correo, "hash-secreta", Rol.ESTUDIANTE, activo);
    }

    private Usuario porRegistrar() {
        return new Usuario(99L, "  Ana  ", "ANA@Mail.com", "secreta", Rol.ESTUDIANTE, false);
    }

    // ------------------------- registrar -------------------------

    @Test
    void registrarLimpiaLosDatosEncriptaYActiva() {
        when(usuarioGateway.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.empty());

        Usuario creado = useCase.registrar(porRegistrar());

        assertNull(creado.getIdUsuario());
        assertEquals("Ana", creado.getNombre());
        assertEquals("ana@mail.com", creado.getCorreo());
        assertEquals("hash-secreta", creado.getClave());
        assertTrue(creado.getActivo());
        verify(usuarioGateway).guardar(creado);
    }

    @Test
    void registrarRechazaDatosNulos() {
        assertThrows(IllegalArgumentException.class, () -> useCase.registrar(null));
    }

    @Test
    void registrarRechazaNombreVacio() {
        Usuario usuario = porRegistrar();
        usuario.setNombre("   ");
        assertThrows(IllegalArgumentException.class, () -> useCase.registrar(usuario));
    }

    @Test
    void registrarRechazaCorreoNulo() {
        Usuario usuario = porRegistrar();
        usuario.setCorreo(null);
        assertThrows(IllegalArgumentException.class, () -> useCase.registrar(usuario));
    }

    @Test
    void registrarRechazaCorreoSinFormato() {
        Usuario usuario = porRegistrar();
        usuario.setCorreo("ana.mail.com");
        assertThrows(IllegalArgumentException.class, () -> useCase.registrar(usuario));
    }

    @Test
    void registrarRechazaClaveCorta() {
        Usuario usuario = porRegistrar();
        usuario.setClave("123");
        assertThrows(IllegalArgumentException.class, () -> useCase.registrar(usuario));
    }

    @Test
    void registrarRechazaClaveNula() {
        Usuario usuario = porRegistrar();
        usuario.setClave(null);
        assertThrows(IllegalArgumentException.class, () -> useCase.registrar(usuario));
    }

    @Test
    void registrarRechazaRolNulo() {
        Usuario usuario = porRegistrar();
        usuario.setRol(null);
        assertThrows(IllegalArgumentException.class, () -> useCase.registrar(usuario));
    }

    @Test
    void registrarRechazaCorreoRepetido() {
        when(usuarioGateway.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(guardado(5L, "ana@mail.com", true)));

        assertThrows(ConflictoException.class, () -> useCase.registrar(porRegistrar()));
        verify(usuarioGateway, never()).guardar(any());
    }

    // ------------------------- buscar / listar -------------------------

    @Test
    void buscarDevuelveElUsuario() {
        Usuario usuario = guardado(1L, "ana@mail.com", true);
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(usuario));

        assertSame(usuario, useCase.buscar(1L));
    }

    @Test
    void buscarRechazaIdNuloOCero() {
        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.buscar(0L));
    }

    @Test
    void buscarLanzaNoEncontrado() {
        when(usuarioGateway.buscarPorId(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> useCase.buscar(7L));
    }

    @Test
    void listarDevuelveLoDelGateway() {
        List<Usuario> usuarios = List.of(guardado(1L, "ana@mail.com", true));
        when(usuarioGateway.listar()).thenReturn(usuarios);

        assertEquals(usuarios, useCase.listar());
    }

    // ------------------------- actualizar / eliminar -------------------------

    @Test
    void actualizarCambiaNombreCorreoYRolPeroNoLaClave() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));
        // El correo nuevo ya es del mismo usuario: está permitido
        when(usuarioGateway.buscarPorCorreo("nuevo@mail.com")).thenReturn(Optional.of(guardado(1L, "nuevo@mail.com", true)));

        Usuario actualizado = useCase.actualizar(1L,
                new Usuario(null, "Ana María", "Nuevo@mail.com", "otra-clave", Rol.DOCENTE, false));

        assertEquals("Ana María", actualizado.getNombre());
        assertEquals("nuevo@mail.com", actualizado.getCorreo());
        assertEquals(Rol.DOCENTE, actualizado.getRol());
        assertEquals("hash-secreta", actualizado.getClave());
        assertTrue(actualizado.getActivo());
    }

    @Test
    void actualizarRechazaDatosNulos() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertThrows(IllegalArgumentException.class, () -> useCase.actualizar(1L, null));
    }

    @Test
    void actualizarRechazaCorreoDeOtroUsuario() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));
        when(usuarioGateway.buscarPorCorreo("luis@mail.com")).thenReturn(Optional.of(guardado(2L, "luis@mail.com", true)));

        assertThrows(ConflictoException.class, () -> useCase.actualizar(1L,
                new Usuario(null, "Ana", "luis@mail.com", null, Rol.ESTUDIANTE, null)));
    }

    @Test
    void eliminarBorraSiExiste() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        useCase.eliminar(1L);

        verify(usuarioGateway).eliminar(1L);
    }

    @Test
    void eliminarLanzaNoEncontrado() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> useCase.eliminar(1L));
        verify(usuarioGateway, never()).eliminar(any());
    }

    // ------------------------- login -------------------------

    @Test
    void loginCorrecto() {
        Usuario usuario = guardado(1L, "ana@mail.com", true);
        when(usuarioGateway.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(usuario));

        assertSame(usuario, useCase.login(" ANA@mail.com ", "secreta"));
    }

    @Test
    void loginRechazaCamposVacios() {
        assertThrows(IllegalArgumentException.class, () -> useCase.login("", "secreta"));
        assertThrows(IllegalArgumentException.class, () -> useCase.login("ana@mail.com", null));
    }

    @Test
    void loginRechazaCorreoDesconocido() {
        when(usuarioGateway.buscarPorCorreo("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> useCase.login("nadie@mail.com", "secreta"));
    }

    @Test
    void loginRechazaClaveIncorrecta() {
        when(usuarioGateway.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertThrows(CredencialesInvalidasException.class, () -> useCase.login("ana@mail.com", "equivocada"));
    }

    @Test
    void loginRechazaCuentaDesactivada() {
        when(usuarioGateway.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(guardado(1L, "ana@mail.com", false)));

        assertThrows(AccesoDenegadoException.class, () -> useCase.login("ana@mail.com", "secreta"));
    }

    // ------------------------- cambiar clave -------------------------

    @Test
    void cambiarClaveGuardaLaNuevaEncriptada() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        Usuario actualizado = useCase.cambiarClave(1L, "secreta", "nueva-clave");

        assertEquals("hash-nueva-clave", actualizado.getClave());
    }

    @Test
    void cambiarClaveRechazaClaveActualVacia() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertThrows(IllegalArgumentException.class, () -> useCase.cambiarClave(1L, " ", "nueva-clave"));
    }

    @Test
    void cambiarClaveRechazaClaveActualIncorrecta() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertThrows(CredencialesInvalidasException.class, () -> useCase.cambiarClave(1L, "equivocada", "nueva-clave"));
    }

    @Test
    void cambiarClaveRechazaClaveNuevaCorta() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertThrows(IllegalArgumentException.class, () -> useCase.cambiarClave(1L, "secreta", "123"));
    }

    @Test
    void cambiarClaveRechazaLaMismaClave() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertThrows(IllegalArgumentException.class, () -> useCase.cambiarClave(1L, "secreta", "secreta"));
        verify(usuarioGateway, never()).guardar(any());
    }

    // ------------------------- activar / desactivar -------------------------

    @Test
    void desactivarCambiaElEstado() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertFalse(useCase.desactivar(1L).getActivo());
    }

    @Test
    void desactivarRechazaCuentaYaDesactivada() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", false)));

        assertThrows(ConflictoException.class, () -> useCase.desactivar(1L));
    }

    @Test
    void activarCambiaElEstado() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", false)));

        assertTrue(useCase.activar(1L).getActivo());
    }

    @Test
    void activarRechazaCuentaYaActiva() {
        when(usuarioGateway.buscarPorId(1L)).thenReturn(Optional.of(guardado(1L, "ana@mail.com", true)));

        assertThrows(ConflictoException.class, () -> useCase.activar(1L));
    }

    // ------------------------- estadísticas -------------------------

    @Test
    void estadisticasCuentaPorRolYPorEstado() {
        Usuario docente = guardado(2L, "luis@mail.com", false);
        docente.setRol(Rol.DOCENTE);
        when(usuarioGateway.listar()).thenReturn(List.of(
                guardado(1L, "ana@mail.com", true), docente, guardado(3L, "eva@mail.com", true)));

        EstadisticasUsuarios estadisticas = useCase.estadisticas();

        assertEquals(3, estadisticas.total());
        assertEquals(2, estadisticas.activos());
        assertEquals(1, estadisticas.inactivos());
        assertEquals(2L, estadisticas.porRol().get(Rol.ESTUDIANTE));
        assertEquals(1L, estadisticas.porRol().get(Rol.DOCENTE));
        assertEquals(0L, estadisticas.porRol().get(Rol.ADMIN));
    }
}
