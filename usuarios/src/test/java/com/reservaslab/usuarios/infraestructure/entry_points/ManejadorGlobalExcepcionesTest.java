package com.reservaslab.usuarios.infraestructure.entry_points;

import com.reservaslab.usuarios.domain.Excepciones.AccesoDenegadoException;
import com.reservaslab.usuarios.domain.Excepciones.ConflictoException;
import com.reservaslab.usuarios.domain.Excepciones.CredencialesInvalidasException;
import com.reservaslab.usuarios.domain.Excepciones.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ManejadorGlobalExcepcionesTest {

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    private void verificar(ResponseEntity<Map<String, Object>> respuesta, HttpStatus esperado) {
        assertEquals(esperado, respuesta.getStatusCode());
        assertEquals(esperado.value(), respuesta.getBody().get("status"));
        assertNotNull(respuesta.getBody().get("mensaje"));
        assertNotNull(respuesta.getBody().get("fecha"));
    }

    @Test
    void cadaExcepcionTieneSuCodigo() {
        verificar(manejador.validacion(new IllegalArgumentException("dato inválido")), HttpStatus.BAD_REQUEST);
        verificar(manejador.jsonInvalido(mock(HttpMessageNotReadableException.class)), HttpStatus.BAD_REQUEST);
        verificar(manejador.credenciales(new CredencialesInvalidasException("clave")), HttpStatus.UNAUTHORIZED);
        verificar(manejador.accesoDenegado(new AccesoDenegadoException("inactiva")), HttpStatus.FORBIDDEN);
        verificar(manejador.noEncontrado(new RecursoNoEncontradoException("no existe")), HttpStatus.NOT_FOUND);
        verificar(manejador.conflicto(new ConflictoException("repetido")), HttpStatus.CONFLICT);
        verificar(manejador.integridad(new DataIntegrityViolationException("único")), HttpStatus.CONFLICT);
    }

    @Test
    void elMensajeEsElDeLaExcepcion() {
        assertEquals("no existe",
                manejador.noEncontrado(new RecursoNoEncontradoException("no existe")).getBody().get("mensaje"));
    }
}
