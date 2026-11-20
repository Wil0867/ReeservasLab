package com.reservaslab.reservas.infraestructure.entry_points;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    // JSON mal formado, fecha u hora con formato incorrecto, o un estado que no existe
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El JSON no es válido. Usa fechas AAAA-MM-DD y horas HH:mm");       // 400
    }

    // Parámetro de la URL con formato incorrecto (?fecha=20-11-2026)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Un parámetro tiene un formato incorrecto. Usa fechas AAAA-MM-DD y horas HH:mm");   // 400
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> parametroFaltante(MissingServletRequestParameterException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta un parámetro obligatorio en la URL");   // 400
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());                      // 404
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaDeNegocio(ReglaDeNegocioException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());                       // 409
    }

    // Por ejemplo, eliminar un laboratorio que tiene reservas en el historial
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException e) {
        return respuesta(HttpStatus.CONFLICT,
                "La operación viola la integridad de los datos (el registro está siendo usado)");   // 409
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
