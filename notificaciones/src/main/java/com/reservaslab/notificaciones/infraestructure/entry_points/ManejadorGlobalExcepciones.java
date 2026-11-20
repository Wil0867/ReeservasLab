package com.reservaslab.notificaciones.infraestructure.entry_points;

import com.reservaslab.notificaciones.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.notificaciones.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce las excepciones de los controladores a códigos HTTP. */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> validacion(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());                    // 400
    }

    // JSON mal formado o un tipo que no existe
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El JSON no es válido. Tipos permitidos: RESERVA, RECORDATORIO, CUENTA, GENERAL");   // 400
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());                      // 404
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaDeNegocio(ReglaDeNegocioException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());                       // 409
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<Map<String, Object>> servicioExterno(ServicioExternoException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());            // 503
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("fecha", LocalDateTime.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", status.getReasonPhrase());
        cuerpo.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
