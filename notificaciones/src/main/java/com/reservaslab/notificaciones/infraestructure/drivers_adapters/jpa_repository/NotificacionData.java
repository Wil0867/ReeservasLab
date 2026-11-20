package com.reservaslab.notificaciones.infraestructure.drivers_adapters.jpa_repository;

import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import com.reservaslab.notificaciones.domain.model.TipoNotificacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notificaciones")
public class NotificacionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    // Referencia lógica: el usuario lo administra el microservicio Usuarios
    @Column(nullable = false)
    private Long idUsuario;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, length = 2000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado;

    @Column(nullable = false)
    private Boolean leida;

    @Column(nullable = false)
    private Integer intentos;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(length = 500)
    private String detalleError;
}
