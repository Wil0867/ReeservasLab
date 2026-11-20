package com.reservaslab.notificaciones.application;

import com.reservaslab.notificaciones.domain.model.gateway.CorreoGateway;
import com.reservaslab.notificaciones.domain.model.gateway.NotificacionGateway;
import com.reservaslab.notificaciones.domain.model.gateway.UsuarioGateway;
import com.reservaslab.notificaciones.domain.usecase.NotificacionUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    public NotificacionUseCase notificacionUseCase(NotificacionGateway notificacionGateway,
                                                   CorreoGateway correoGateway,
                                                   UsuarioGateway usuarioGateway,
                                                   Clock clock) {
        return new NotificacionUseCase(notificacionGateway, correoGateway, usuarioGateway, clock);
    }
}
