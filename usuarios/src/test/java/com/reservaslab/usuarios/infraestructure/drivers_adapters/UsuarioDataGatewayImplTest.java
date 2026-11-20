package com.reservaslab.usuarios.infraestructure.drivers_adapters;

import com.reservaslab.usuarios.domain.model.Rol;
import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.infraestructure.drivers_adapters.jpa_repository.UsuarioData;
import com.reservaslab.usuarios.infraestructure.drivers_adapters.jpa_repository.UsuarioDataGatewayImpl;
import com.reservaslab.usuarios.infraestructure.drivers_adapters.jpa_repository.UsuarioDataJpaRepository;
import com.reservaslab.usuarios.infraestructure.mapper.MapperUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioDataGatewayImplTest {

    private UsuarioDataJpaRepository repository;
    private UsuarioDataGatewayImpl gateway;
    private UsuarioData data;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioDataJpaRepository.class);
        gateway = new UsuarioDataGatewayImpl(repository, new MapperUsuario());
        data = new UsuarioData(1L, "Ana", "ana@mail.com", "hash", Rol.ESTUDIANTE, true);
    }

    @Test
    void guardarConvierteGuardaYDevuelveConId() {
        when(repository.save(any(UsuarioData.class))).thenReturn(data);

        Usuario guardado = gateway.guardar(new Usuario(null, "Ana", "ana@mail.com", "hash", Rol.ESTUDIANTE, true));

        assertEquals(1L, guardado.getIdUsuario());
    }

    @Test
    void buscarPorIdYPorCorreo() {
        when(repository.findById(1L)).thenReturn(Optional.of(data));
        when(repository.findById(2L)).thenReturn(Optional.empty());
        when(repository.findByCorreo("ana@mail.com")).thenReturn(Optional.of(data));

        assertEquals("Ana", gateway.buscarPorId(1L).orElseThrow().getNombre());
        assertTrue(gateway.buscarPorId(2L).isEmpty());
        assertEquals(1L, gateway.buscarPorCorreo("ana@mail.com").orElseThrow().getIdUsuario());
    }

    @Test
    void listarConvierteCadaFila() {
        when(repository.findAll()).thenReturn(List.of(data, data));

        assertEquals(2, gateway.listar().size());
    }

    @Test
    void eliminarDelegaEnElRepositorio() {
        gateway.eliminar(1L);

        verify(repository).deleteById(1L);
    }
}
