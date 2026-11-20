package com.reservaslab.usuarios.infraestructure.entry_points;

import com.reservaslab.usuarios.domain.model.EstadisticasUsuarios;
import com.reservaslab.usuarios.domain.model.Rol;
import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.domain.usecase.UsuarioUseCase;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.ActualizarRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.CambioClaveRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.LoginRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.RegistroRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.UsuarioResponse;
import com.reservaslab.usuarios.infraestructure.mapper.MapperUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UsuarioControllerTest {

    private UsuarioUseCase useCase;
    private UsuarioController controller;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        useCase = mock(UsuarioUseCase.class);
        controller = new UsuarioController(useCase, new MapperUsuario());
        ana = new Usuario(1L, "Ana", "ana@mail.com", "hash", Rol.ESTUDIANTE, true);
    }

    @Test
    void registrarResponde201SinLaClave() {
        when(useCase.registrar(any(Usuario.class))).thenReturn(ana);

        ResponseEntity<UsuarioResponse> respuesta =
                controller.registrar(new RegistroRequest("Ana", "ana@mail.com", "secreta", Rol.ESTUDIANTE));

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(new UsuarioResponse(1L, "Ana", "ana@mail.com", Rol.ESTUDIANTE, true), respuesta.getBody());
    }

    @Test
    void listarResponde200() {
        when(useCase.listar()).thenReturn(List.of(ana));

        ResponseEntity<List<UsuarioResponse>> respuesta = controller.listar();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(1, respuesta.getBody().size());
    }

    @Test
    void buscarYValidarResponden200() {
        when(useCase.buscar(1L)).thenReturn(ana);

        assertEquals("ana@mail.com", controller.buscar(1L).getBody().correo());
        assertEquals(HttpStatus.OK, controller.validar(1L).getStatusCode());
        assertTrue(controller.validar(1L).getBody().activo());
    }

    @Test
    void actualizarResponde200() {
        when(useCase.actualizar(eq(1L), any(Usuario.class))).thenReturn(ana);

        ResponseEntity<UsuarioResponse> respuesta =
                controller.actualizar(1L, new ActualizarRequest("Ana", "ana@mail.com", Rol.ESTUDIANTE));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(1L, respuesta.getBody().idUsuario());
    }

    @Test
    void eliminarResponde200ConMensaje() {
        ResponseEntity<Map<String, Object>> respuesta = controller.eliminar(1L);

        verify(useCase).eliminar(1L);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().containsKey("mensaje"));
    }

    @Test
    void loginRespondeBienvenidaYUsuario() {
        when(useCase.login("ana@mail.com", "secreta")).thenReturn(ana);

        ResponseEntity<Map<String, Object>> respuesta = controller.login(new LoginRequest("ana@mail.com", "secreta"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Bienvenido, Ana", respuesta.getBody().get("mensaje"));
        assertInstanceOf(UsuarioResponse.class, respuesta.getBody().get("usuario"));
    }

    @Test
    void cambiarClaveResponde200() {
        ResponseEntity<Map<String, Object>> respuesta =
                controller.cambiarClave(1L, new CambioClaveRequest("secreta", "nueva-clave"));

        verify(useCase).cambiarClave(1L, "secreta", "nueva-clave");
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }

    @Test
    void activarYDesactivarResponden200() {
        when(useCase.desactivar(1L)).thenReturn(new Usuario(1L, "Ana", "ana@mail.com", "hash", Rol.ESTUDIANTE, false));
        when(useCase.activar(1L)).thenReturn(ana);

        assertFalse(controller.desactivar(1L).getBody().activo());
        assertTrue(controller.activar(1L).getBody().activo());
    }

    @Test
    void estadisticasResponde200() {
        EstadisticasUsuarios estadisticas = new EstadisticasUsuarios(1, 1, 0, Map.of(Rol.ESTUDIANTE, 1L));
        when(useCase.estadisticas()).thenReturn(estadisticas);

        assertSame(estadisticas, controller.estadisticas().getBody());
    }
}
