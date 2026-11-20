package com.reservaslab.usuarios.application;

import com.reservaslab.usuarios.domain.model.gateway.EncriptarGateway;
import com.reservaslab.usuarios.domain.model.gateway.UsuarioGateway;
import com.reservaslab.usuarios.domain.usecase.UsuarioUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UsuarioConfig {

    @Bean
    public UsuarioUseCase usuarioUseCase(UsuarioGateway usuarioGateway, EncriptarGateway encriptarGateway) {
        return new UsuarioUseCase(usuarioGateway, encriptarGateway);
    }
}
