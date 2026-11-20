package com.reservaslab.usuarios.application;

import com.reservaslab.usuarios.domain.model.gateway.EncriptarGateway;
import com.reservaslab.usuarios.domain.model.gateway.UsuarioGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ConfiguracionTest {

    @Test
    void creaLosBeans() {
        assertNotNull(new EncriptarConfig().passwordEncoder());
        assertNotNull(new UsuarioConfig().usuarioUseCase(mock(UsuarioGateway.class), mock(EncriptarGateway.class)));
    }
}
