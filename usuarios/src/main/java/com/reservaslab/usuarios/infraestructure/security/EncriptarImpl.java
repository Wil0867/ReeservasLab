package com.reservaslab.usuarios.infraestructure.security;

import com.reservaslab.usuarios.domain.model.gateway.EncriptarGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EncriptarImpl implements EncriptarGateway {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encriptar(String clave) {
        return passwordEncoder.encode(clave);
    }

    @Override
    public boolean verificar(String clave, String claveEncriptada) {
        return passwordEncoder.matches(clave, claveEncriptada);
    }
}
