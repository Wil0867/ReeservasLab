package com.reservaslab.reservas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Laboratorio {
    private Long idLaboratorio;
    private String nombre;
    private String ubicacion;
    private Integer capacidad;          // Máximo de personas por reserva
    private EstadoLaboratorio estado;
}
