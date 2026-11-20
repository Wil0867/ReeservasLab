package com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LaboratorioDataJpaRepository extends JpaRepository<LaboratorioData, Long> {

    // SELECT ... FOR UPDATE: bloquea la fila del laboratorio hasta que termine la transacción.
    // Dos reservas simultáneas del mismo laboratorio pasan una detrás de la otra.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LaboratorioData l WHERE l.idLaboratorio = :idLaboratorio")
    Optional<LaboratorioData> bloquearPorId(@Param("idLaboratorio") Long idLaboratorio);
}
