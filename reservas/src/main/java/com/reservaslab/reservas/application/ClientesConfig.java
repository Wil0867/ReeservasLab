package com.reservaslab.reservas.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/** Clientes HTTP hacia los otros microservicios y la API de festivos, y el reloj del sistema. */
@Configuration
public class ClientesConfig {

    @Bean
    public RestClient usuariosRestClient(@Value("${usuarios.service.url}") String url) {
        return crearCliente(url);
    }

    @Bean
    public RestClient notificacionesRestClient(@Value("${notificaciones.service.url}") String url) {
        return crearCliente(url);
    }

    @Bean
    public RestClient festivosRestClient(@Value("${festivos.api.url}") String url) {
        return crearCliente(url);
    }

    /** Hora de Colombia, sin importar en qué país esté el servidor donde se despliegue. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    private RestClient crearCliente(String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // Amplio a propósito: en el plan gratuito de Render un servicio dormido tarda en despertar
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }
}
