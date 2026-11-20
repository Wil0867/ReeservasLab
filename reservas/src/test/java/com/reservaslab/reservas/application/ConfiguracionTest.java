package com.reservaslab.reservas.application;

import com.reservaslab.reservas.domain.model.gateway.FestivoGateway;
import com.reservaslab.reservas.domain.model.gateway.LaboratorioGateway;
import com.reservaslab.reservas.domain.model.gateway.NotificacionGateway;
import com.reservaslab.reservas.domain.model.gateway.ReservaGateway;
import com.reservaslab.reservas.domain.model.gateway.UsuarioGateway;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ConfiguracionTest {

    @Test
    void creaLosClientesYElReloj() {
        ClientesConfig config = new ClientesConfig();

        assertNotNull(config.usuariosRestClient("http://localhost:8080"));
        assertNotNull(config.notificacionesRestClient("http://localhost:8084"));
        assertNotNull(config.festivosRestClient("https://date.nager.at"));
        assertEquals(ZoneId.of("America/Bogota"), config.clock().getZone());
    }

    @Test
    void creaLosCasosDeUso() {
        UseCaseConfig config = new UseCaseConfig();
        ReservaGateway reservaGateway = mock(ReservaGateway.class);
        LaboratorioGateway laboratorioGateway = mock(LaboratorioGateway.class);
        FestivoGateway festivoGateway = mock(FestivoGateway.class);

        assertNotNull(config.laboratorioUseCase(laboratorioGateway, reservaGateway, festivoGateway));
        assertNotNull(config.reservaUseCase(reservaGateway, laboratorioGateway, mock(UsuarioGateway.class),
                festivoGateway, mock(NotificacionGateway.class), Clock.systemUTC()));
    }
}
