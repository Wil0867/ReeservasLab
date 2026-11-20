package com.reservaslab.reservas.domain.model.gateway;

import java.time.LocalDate;

/** API externa de festivos. Si no responde, lanza ServicioExternoException. */
public interface FestivoGateway {
    boolean esFestivo(LocalDate fecha);
}
