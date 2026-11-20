package com.reservaslab.reservas.application;

import com.reservaslab.reservas.domain.model.gateway.FestivoGateway;
import com.reservaslab.reservas.domain.model.gateway.LaboratorioGateway;
import com.reservaslab.reservas.domain.model.gateway.NotificacionGateway;
import com.reservaslab.reservas.domain.model.gateway.ReservaGateway;
import com.reservaslab.reservas.domain.model.gateway.UsuarioGateway;
import com.reservaslab.reservas.domain.usecase.LaboratorioUseCase;
import com.reservaslab.reservas.domain.usecase.ReservaUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    public LaboratorioUseCase laboratorioUseCase(LaboratorioGateway laboratorioGateway,
                                                 ReservaGateway reservaGateway,
                                                 FestivoGateway festivoGateway) {
        return new LaboratorioUseCase(laboratorioGateway, reservaGateway, festivoGateway);
    }

    @Bean
    public ReservaUseCase reservaUseCase(ReservaGateway reservaGateway,
                                         LaboratorioGateway laboratorioGateway,
                                         UsuarioGateway usuarioGateway,
                                         FestivoGateway festivoGateway,
                                         NotificacionGateway notificacionGateway,
                                         Clock clock) {
        return new ReservaUseCase(reservaGateway, laboratorioGateway, usuarioGateway,
                festivoGateway, notificacionGateway, clock);
    }
}
