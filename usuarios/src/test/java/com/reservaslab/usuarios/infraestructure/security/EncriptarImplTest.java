package com.reservaslab.usuarios.infraestructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class EncriptarImplTest {

    private final EncriptarImpl encriptar = new EncriptarImpl(new BCryptPasswordEncoder());

    @Test
    void encriptaYVerificaLaClave() {
        String hash = encriptar.encriptar("secreta");

        assertNotEquals("secreta", hash);
        assertTrue(encriptar.verificar("secreta", hash));
        assertFalse(encriptar.verificar("otra", hash));
    }
}
