package com.reservaslab.reservas.infraestructure.drivers_adapters.external_repository;

import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import com.reservaslab.reservas.domain.model.gateway.FestivoGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * API EXTERNA 1: Nager.Date (https://date.nager.at), festivos por país y año.
 * Los festivos de un año no cambian, así que se guardan en memoria: una sola llamada por año.
 */
@Component
public class FestivoGatewayImpl implements FestivoGateway {

    private final RestClient festivosRestClient;
    private final String pais;
    private final Map<Integer, Set<LocalDate>> festivosPorAnio = new ConcurrentHashMap<>();

    public FestivoGatewayImpl(@Qualifier("festivosRestClient") RestClient festivosRestClient,
                              @Value("${festivos.api.pais}") String pais) {
        this.festivosRestClient = festivosRestClient;
        this.pais = pais;
    }

    // La API responde más campos; solo interesa la fecha ("2026-12-25")
    public record FestivoResponse(String date, String localName) {
    }

    @Override
    public boolean esFestivo(LocalDate fecha) {
        return festivosPorAnio.computeIfAbsent(fecha.getYear(), this::consultarAnio).contains(fecha);
    }

    private Set<LocalDate> consultarAnio(Integer anio) {
        try {
            FestivoResponse[] festivos = festivosRestClient.get()
                    .uri("/api/v3/PublicHolidays/{anio}/{pais}", anio, pais)
                    .retrieve()
                    .body(FestivoResponse[].class);
            if (festivos == null) {
                return Set.of();
            }
            return Arrays.stream(festivos)
                    .map(festivo -> LocalDate.parse(festivo.date()))
                    .collect(Collectors.toSet());
        } catch (RestClientException e) {
            throw new ServicioExternoException("No fue posible consultar la API de festivos", e);
        }
    }
}
