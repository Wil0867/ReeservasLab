package com.reservaslab.notificaciones.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
public class ClientesConfig {

    @Bean
    public RestClient usuariosRestClient(@Value("${usuarios.service.url}") String url) {
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica(60))     // en Render gratis, Usuarios puede tardar en despertar
                .build();
    }

    /** Brevo identifica la cuenta con la cabecera "api-key" en cada petición. */
    @Bean
    public RestClient brevoRestClient(@Value("${brevo.api.url}") String url,
                                      @Value("${brevo.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", "application/json")
                .requestFactory(fabrica(15))
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    private SimpleClientHttpRequestFactory fabrica(int segundosDeLectura) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(segundosDeLectura));
        return factory;
    }
}
