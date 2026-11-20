package com.reservaslab.notificaciones.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {
    private Long idNotificacion;
    private Long idUsuario;
    private String destinatario;
    private String asunto;
    private String mensaje;
    private TipoNotificacion tipo;
    private EstadoNotificacion estado;
    private Boolean leida;
    private Integer intentos;
    private LocalDateTime fechaCreacion;
    private String detalleError;
}
