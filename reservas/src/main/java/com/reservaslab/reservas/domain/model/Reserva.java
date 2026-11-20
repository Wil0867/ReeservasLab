package com.reservaslab.reservas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Reserva {
    private Long idReserva;
    private Long idUsuario;             // Solo el id: el usuario pertenece al microservicio Usuarios
    private Long idLaboratorio;
    private String nombreLaboratorio;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer cantidadPersonas;
    private String motivo;
    private EstadoReserva estado;
}
