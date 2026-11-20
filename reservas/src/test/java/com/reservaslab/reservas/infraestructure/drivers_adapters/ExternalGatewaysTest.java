package com.reservaslab.reservas.infraestructure.drivers_adapters;

import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.UsuarioInfo;
import com.reservaslab.reservas.infraestructure.drivers_adapters.external_repository.FestivoGatewayImpl;
import com.reservaslab.reservas.infraestructure.drivers_adapters.external_repository.NotificacionGatewayImpl;
import com.reservaslab.reservas.infraestructure.drivers_adapters.external_repository.UsuarioGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Adaptadores REST. MockRestServiceServer finge el servidor remoto:
 * no sale ninguna petición real a internet ni a los otros microservicios.
 */
class ExternalGatewaysTest {

    private static final String BASE = "http://servidor";

    private MockRestServiceServer servidor;
    private RestClient restClient;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    // ---------------- Usuarios ----------------

    @Test
    void usuarioExistente() {
        servidor.expect(requestTo(BASE + "/api/usuarios/1/validar"))
                .andRespond(withSuccess(
                        "{\"idUsuario\":1,\"nombre\":\"Ana\",\"correo\":\"ana@correo.com\",\"rol\":\"ESTUDIANTE\",\"activo\":true}",
                        MediaType.APPLICATION_JSON));

        UsuarioInfo usuario = new UsuarioGatewayImpl(restClient).consultar(1L).orElseThrow();

        assertEquals(1L, usuario.idUsuario());
        assertEquals("Ana", usuario.nombre());
        assertTrue(usuario.activo());
        servidor.verify();
    }

    @Test
    void usuarioInexistenteDevuelveVacio() {
        servidor.expect(requestTo(BASE + "/api/usuarios/9/validar")).andRespond(withResourceNotFound());

        assertTrue(new UsuarioGatewayImpl(restClient).consultar(9L).isEmpty());
    }

    @Test
    void usuariosCaidoLanzaServicioExterno() {
        servidor.expect(requestTo(BASE + "/api/usuarios/1/validar")).andRespond(withServerError());

        assertThrows(ServicioExternoException.class, () -> new UsuarioGatewayImpl(restClient).consultar(1L));
    }

    // ---------------- Festivos (API externa) ----------------

    @Test
    void festivosConsultaLaApiUnaSolaVezPorAnio() {
        // ExpectedCount.once(): si el adaptador llamara dos veces, la prueba falla
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v3/PublicHolidays/2026/CO"))
                .andRespond(withSuccess(
                        "[{\"date\":\"2026-12-25\",\"localName\":\"Navidad\",\"countryCode\":\"CO\"},"
                                + "{\"date\":\"2026-01-01\",\"localName\":\"Año Nuevo\",\"countryCode\":\"CO\"}]",
                        MediaType.APPLICATION_JSON));
        FestivoGatewayImpl gateway = new FestivoGatewayImpl(restClient, "CO");

        assertTrue(gateway.esFestivo(LocalDate.of(2026, 12, 25)));
        assertFalse(gateway.esFestivo(LocalDate.of(2026, 11, 20)));     // sale de la memoria, sin otra llamada
        servidor.verify();
    }

    @Test
    void festivosCaidoLanzaServicioExterno() {
        servidor.expect(requestTo(BASE + "/api/v3/PublicHolidays/2026/CO")).andRespond(withServerError());

        assertThrows(ServicioExternoException.class,
                () -> new FestivoGatewayImpl(restClient, "CO").esFestivo(LocalDate.of(2026, 12, 25)));
    }

    // ---------------- Notificaciones ----------------

    @Test
    void notificarEnviaElJsonEsperado() {
        servidor.expect(requestTo(BASE + "/api/notificaciones/enviar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"asunto\":\"Reserva registrada #10\"")))
                .andExpect(content().string(containsString("\"tipo\":\"RESERVA\"")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new NotificacionGatewayImpl(restClient).notificar(1L, "Reserva registrada #10", "Detalle");

        servidor.verify();
    }

    @Test
    void notificacionesCaidoLanzaServicioExterno() {
        servidor.expect(requestTo(BASE + "/api/notificaciones/enviar")).andRespond(withServerError());

        assertThrows(ServicioExternoException.class,
                () -> new NotificacionGatewayImpl(restClient).notificar(1L, "Asunto", "Mensaje"));
    }
}
