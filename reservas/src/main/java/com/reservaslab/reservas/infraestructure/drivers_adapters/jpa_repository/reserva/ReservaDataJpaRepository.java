package com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva;

import com.reservaslab.reservas.domain.model.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ReservaDataJpaRepository extends JpaRepository<ReservaData, Long> {

    // Dos horarios se cruzan si uno empieza antes de que el otro termine, y termina después de que el otro empieza
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END
            FROM ReservaData r
            WHERE r.laboratorio.idLaboratorio = :idLaboratorio
              AND r.fecha = :fecha
              AND r.estado IN :estados
              AND r.horaInicio < :horaFin
              AND r.horaFin > :horaInicio
            """)
    boolean existeCruce(@Param("idLaboratorio") Long idLaboratorio,
                        @Param("fecha") LocalDate fecha,
                        @Param("estados") Collection<EstadoReserva> estados,
                        @Param("horaInicio") LocalTime horaInicio,
                        @Param("horaFin") LocalTime horaFin);

    @Query("""
            SELECT DISTINCT r.laboratorio.idLaboratorio
            FROM ReservaData r
            WHERE r.fecha = :fecha
              AND r.estado IN :estados
              AND r.horaInicio < :horaFin
              AND r.horaFin > :horaInicio
            """)
    List<Long> idsLaboratoriosConCruce(@Param("fecha") LocalDate fecha,
                                       @Param("estados") Collection<EstadoReserva> estados,
                                       @Param("horaInicio") LocalTime horaInicio,
                                       @Param("horaFin") LocalTime horaFin);

    List<ReservaData> findByIdUsuarioOrderByFechaDescHoraInicioDesc(Long idUsuario);

    List<ReservaData> findByLaboratorio_IdLaboratorioAndFechaAndEstadoInOrderByHoraInicioAsc(
            Long idLaboratorio, LocalDate fecha, Collection<EstadoReserva> estados);

    boolean existsByLaboratorio_IdLaboratorioAndEstadoIn(Long idLaboratorio, Collection<EstadoReserva> estados);
}
