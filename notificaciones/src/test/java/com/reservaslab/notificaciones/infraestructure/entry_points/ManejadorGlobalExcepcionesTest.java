package com.reservaslab.notificaciones.infraestructure.entry_points;

import com.reservaslab.notificaciones.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.notificaciones.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ManejadorGlobalExcepcionesTest {

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    @Test
    void cadaExcepcionTieneSuCodigo() {
        ResponseEntity<Map<String, Object>> validacion =
                manejador.validacion(new IllegalArgumentException("dato inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, validacion.getStatusCode());
        assertEquals("dato inválido", validacion.getBody().get("mensaje"));
        assertEquals(400, validacion.getBody().get("status"));
        assertNotNull(validacion.getBody().get("fecha"));
        assertEquals(HttpStatus.BAD_REQUEST,
                manejador.jsonInvalido(mock(HttpMessageNotReadableException.class)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                manejador.noEncontrado(new RecursoNoEncontradoException("no existe")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                manejador.reglaDeNegocio(new ReglaDeNegocioException("ya leída")).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                manejador.servicioExterno(new ServicioExternoException("caído", null)).getStatusCode());
    }
}
