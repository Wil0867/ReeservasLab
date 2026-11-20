package com.reservaslab.notificaciones.application;

import com.reservaslab.notificaciones.domain.model.gateway.CorreoGateway;
import com.reservaslab.notificaciones.domain.model.gateway.NotificacionGateway;
import com.reservaslab.notificaciones.domain.model.gateway.UsuarioGateway;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ConfiguracionTest {

    @Test
    void creaLosBeans() {
        ClientesConfig clientes = new ClientesConfig();

        assertNotNull(clientes.usuariosRestClient("http://localhost:8080"));
        assertNotNull(clientes.brevoRestClient("https://api.brevo.com", "clave-de-prueba"));
        assertEquals(ZoneId.of("America/Bogota"), clientes.clock().getZone());
        assertNotNull(new UseCaseConfig().notificacionUseCase(mock(NotificacionGateway.class),
                mock(CorreoGateway.class), mock(UsuarioGateway.class), Clock.systemUTC()));
    }
}
