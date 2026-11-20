package com.reservaslab.notificaciones.infraestructure.entry_points;

import com.reservaslab.notificaciones.domain.usecase.NotificacionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Envía el correo en otro hilo, después de que el controlador ya respondió 202.
 * El resultado (ENVIADA o FALLIDA) queda guardado en la notificación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioCorreoAsincrono {

    private final NotificacionUseCase notificacionUseCase;

    @Async
    public void procesar(Long idNotificacion) {
        try {
            notificacionUseCase.procesarEnvio(idNotificacion);
        } catch (RuntimeException e) {
            // Queda PENDIENTE; se registra para revisarla
            log.error("No se pudo procesar el envío de la notificación {}", idNotificacion, e);
        }
    }
}
