package com.reservaslab.reservas.infraestructure.entry_points;

import com.reservaslab.reservas.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.reservas.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.reservas.domain.Excepciones.ServicioExternoException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ManejadorGlobalExcepcionesTest {

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    @Test
    void erroresDelClienteResponden400() {
        ResponseEntity<Map<String, Object>> validacion =
                manejador.validacion(new IllegalArgumentException("dato inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, validacion.getStatusCode());
        assertEquals("dato inválido", validacion.getBody().get("mensaje"));
        assertEquals(400, validacion.getBody().get("status"));
        assertNotNull(validacion.getBody().get("fecha"));
        assertEquals(HttpStatus.BAD_REQUEST,
                manejador.jsonInvalido(mock(HttpMessageNotReadableException.class)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                manejador.parametroInvalido(mock(MethodArgumentTypeMismatchException.class)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                manejador.parametroFaltante(mock(MissingServletRequestParameterException.class)).getStatusCode());
    }

    @Test
    void noEncontradoResponde404() {
        ResponseEntity<Map<String, Object>> respuesta =
                manejador.noEncontrado(new RecursoNoEncontradoException("no existe"));

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("no existe", respuesta.getBody().get("mensaje"));
    }

    @Test
    void reglasEIntegridadResponden409() {
        assertEquals(HttpStatus.CONFLICT,
                manejador.reglaDeNegocio(new ReglaDeNegocioException("ocupado")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                manejador.integridad(new DataIntegrityViolationException("fk")).getStatusCode());
    }

    @Test
    void servicioExternoResponde503() {
        ResponseEntity<Map<String, Object>> respuesta =
                manejador.servicioExterno(new ServicioExternoException("caído", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
        assertEquals("caído", respuesta.getBody().get("mensaje"));
    }
}
