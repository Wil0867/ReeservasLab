package com.reservaslab.usuarios.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Usuario {
    private Long idUsuario;
    private String nombre;
    private String correo;
    private String clave;       // Siempre encriptada; nunca sale en las respuestas
    private Rol rol;
    private Boolean activo;     // Una cuenta desactivada no puede iniciar sesión ni reservar
}
