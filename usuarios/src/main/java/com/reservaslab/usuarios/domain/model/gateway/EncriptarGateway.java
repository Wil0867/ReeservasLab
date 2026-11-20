package com.reservaslab.usuarios.domain.model.gateway;

public interface EncriptarGateway {
    String encriptar(String clave);

    boolean verificar(String clave, String claveEncriptada);
}
