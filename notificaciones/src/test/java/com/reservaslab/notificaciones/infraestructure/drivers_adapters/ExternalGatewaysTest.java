package com.reservaslab.notificaciones.infraestructure.drivers_adapters;

import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import com.reservaslab.notificaciones.infraestructure.drivers_adapters.external_repository.BrevoCorreoGatewayImpl;
import com.reservaslab.notificaciones.infraestructure.drivers_adapters.external_repository.UsuarioGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Adaptadores REST. MockRestServiceServer finge el servidor remoto:
 * en las pruebas no se envía ningún correo real ni se llama a Usuarios.
 */
class ExternalGatewaysTest {

    private static final String BASE = "http://servidor";

    private MockRestServiceServer servidor;
    private RestClient restClient;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("api-key", "clave-de-prueba");
        servidor = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    // ---------------- Usuarios ----------------

    @Test
    void correoDeUnUsuarioExistente() {
        servidor.expect(requestTo(BASE + "/api/usuarios/1/validar"))
                .andRespond(withSuccess(
                        "{\"idUsuario\":1,\"nombre\":\"Ana\",\"correo\":\"ana@correo.com\",\"rol\":\"ESTUDIANTE\",\"activo\":true}",
                        MediaType.APPLICATION_JSON));

        assertEquals("ana@correo.com", new UsuarioGatewayImpl(restClient).consultarCorreo(1L).orElseThrow());
    }

    @Test
    void usuarioInexistenteDevuelveVacio() {
        servidor.expect(requestTo(BASE + "/api/usuarios/9/validar")).andRespond(withResourceNotFound());

        assertTrue(new UsuarioGatewayImpl(restClient).consultarCorreo(9L).isEmpty());
    }

    @Test
    void usuariosCaidoLanzaServicioExterno() {
        servidor.expect(requestTo(BASE + "/api/usuarios/1/validar")).andRespond(withServerError());

        assertThrows(ServicioExternoException.class, () -> new UsuarioGatewayImpl(restClient).consultarCorreo(1L));
    }

    // ---------------- Brevo (API externa) ----------------

    @Test
    void brevoRecibeElJsonYLaClave() {
        servidor.expect(requestTo(BASE + "/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "clave-de-prueba"))
                // Cada campo por separado: el orden de los campos dentro del JSON no importa
                .andExpect(content().string(containsString("\"name\":\"Reservas\"")))
                .andExpect(content().string(containsString("\"email\":\"lab@correo.com\"")))
                .andExpect(content().string(containsString("\"to\":[{\"email\":\"ana@correo.com\"}]")))
                .andExpect(content().string(containsString("\"subject\":\"Asunto\"")))
                .andExpect(content().string(containsString("\"textContent\":\"Mensaje\"")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"<abc@smtp-relay.mailin.fr>\"}"));

        new BrevoCorreoGatewayImpl(restClient, "lab@correo.com", "Reservas")
                .enviar("ana@correo.com", "Asunto", "Mensaje");

        servidor.verify();
    }

    @Test
    void brevoRechazaLanzaServicioExterno() {
        servidor.expect(requestTo(BASE + "/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"unauthorized\",\"message\":\"Key not found\"}"));

        ServicioExternoException error = assertThrows(ServicioExternoException.class,
                () -> new BrevoCorreoGatewayImpl(restClient, "lab@correo.com", "Reservas")
                        .enviar("ana@correo.com", "Asunto", "Mensaje"));
        assertTrue(error.getMessage().startsWith("La API de correo rechazó el envío"));
    }
}
