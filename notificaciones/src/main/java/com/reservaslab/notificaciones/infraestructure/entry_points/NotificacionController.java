package com.reservaslab.notificaciones.infraestructure.entry_points;

import com.reservaslab.notificaciones.domain.model.EstadisticasNotificaciones;
import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.domain.usecase.NotificacionUseCase;
import com.reservaslab.notificaciones.infraestructure.entry_points.dto.NotificacionRequest;
import com.reservaslab.notificaciones.infraestructure.mapper.MapperNotificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionUseCase notificacionUseCase;
    private final MapperNotificacion mapperNotificacion;

    // ------------------------- Consulta y borrado básicos -------------------------

    @GetMapping("/lista")
    public ResponseEntity<List<Notificacion>> listar() {
        return ResponseEntity.ok(notificacionUseCase.listar());
    }

    @GetMapping("/buscar/{idNotificacion}")
    public ResponseEntity<Notificacion> buscar(@PathVariable Long idNotificacion) {
        return ResponseEntity.ok(notificacionUseCase.buscar(idNotificacion));
    }

    @DeleteMapping("/eliminar/{idNotificacion}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long idNotificacion) {
        notificacionUseCase.eliminar(idNotificacion);
        return ResponseEntity.ok(Map.of("mensaje", "Notificación eliminada correctamente"));
    }

    // ------------------- APIs de negocio (no CRUD) -------------------

    // 1. Enviar un correo y dejarlo en el historial (el estado dice si salió: ENVIADA o FALLIDA)
    @PostMapping("/enviar")
    public ResponseEntity<Notificacion> enviar(@RequestBody NotificacionRequest request) {
        Notificacion resultado = notificacionUseCase.enviar(mapperNotificacion.toNotificacion(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    // 2. Reintentar una notificación fallida (máximo 3 intentos)
    @PostMapping("/{idNotificacion}/reintentar")
    public ResponseEntity<Notificacion> reintentar(@PathVariable Long idNotificacion) {
        return ResponseEntity.ok(notificacionUseCase.reintentar(idNotificacion));
    }

    // 3. Marcar como leída
    @PatchMapping("/{idNotificacion}/leida")
    public ResponseEntity<Notificacion> marcarLeida(@PathVariable Long idNotificacion) {
        return ResponseEntity.ok(notificacionUseCase.marcarLeida(idNotificacion));
    }

    // 4. Historial de un usuario
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Notificacion>> historial(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(notificacionUseCase.listarPorUsuario(idUsuario));
    }

    // 5. Notificaciones sin leer de un usuario
    @GetMapping("/usuario/{idUsuario}/no-leidas")
    public ResponseEntity<List<Notificacion>> noLeidas(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(notificacionUseCase.listarNoLeidas(idUsuario));
    }

    // 6. Notificaciones que no se pudieron enviar
    @GetMapping("/fallidas")
    public ResponseEntity<List<Notificacion>> fallidas() {
        return ResponseEntity.ok(notificacionUseCase.listarFallidas());
    }

    // 7. Resumen: totales por estado y por tipo
    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasNotificaciones> estadisticas() {
        return ResponseEntity.ok(notificacionUseCase.estadisticas());
    }
}
