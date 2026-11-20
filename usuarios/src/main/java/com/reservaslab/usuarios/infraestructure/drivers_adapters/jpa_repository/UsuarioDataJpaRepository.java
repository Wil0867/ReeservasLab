package com.reservaslab.usuarios.infraestructure.drivers_adapters.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioDataJpaRepository extends JpaRepository<UsuarioData, Long> {

    Optional<UsuarioData> findByCorreo(String correo);
}
