package com.reservaslab.notificaciones.infraestructure.entry_points;

import com.reservaslab.notificaciones.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.notificaciones.domain.usecase.NotificacionUseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnvioCorreoAsincronoTest {

    private final NotificacionUseCase useCase = mock(NotificacionUseCase.class);
    private final EnvioCorreoAsincrono envio = new EnvioCorreoAsincrono(useCase);

    @Test
    void procesaElEnvioDeLaNotificacion() {
        envio.procesar(7L);

        verify(useCase).procesarEnvio(7L);
    }

    @Test
    void unErrorNoSePropagaAlHiloQueLlamo() {
        when(useCase.procesarEnvio(8L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        assertDoesNotThrow(() -> envio.procesar(8L));
    }
}
