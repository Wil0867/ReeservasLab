package com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.reserva;

import com.reservaslab.reservas.domain.model.EstadoReserva;
import com.reservaslab.reservas.infraestructure.drivers_adapters.jpa_repository.laboratorio.LaboratorioData;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reservas")
public class ReservaData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    // Referencia lógica: el usuario lo administra el microservicio Usuarios
    @Column(nullable = false)
    private Long idUsuario;

    // Muchas reservas pertenecen a un laboratorio (clave foránea id_laboratorio)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_laboratorio", nullable = false)
    private LaboratorioData laboratorio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private Integer cantidadPersonas;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;
}
