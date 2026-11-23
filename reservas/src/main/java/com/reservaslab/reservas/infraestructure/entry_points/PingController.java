package com.reservaslab.reservas.infraestructure.entry_points;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Respuesta liviana para el "keep-alive" y el health check de Render.
 * No consulta la base de datos, así Neon puede dormir cuando no hay uso real.
 */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public Map<String, String> ping() {
        return Map.of("estado", "ok", "servicio", "reservas");
    }
}
