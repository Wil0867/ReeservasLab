package com.reservaslab.reservas.infraestructure.entry_points;

import com.reservaslab.reservas.domain.model.Reserva;
import com.reservaslab.reservas.domain.usecase.ReservaUseCase;
import com.reservaslab.reservas.infraestructure.entry_points.dto.ReservaRequest;
import com.reservaslab.reservas.infraestructure.mapper.MapperReserva;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaUseCase reservaUseCase;
    private final MapperReserva mapperReserva;

    @PostMapping("/crear")
    public ResponseEntity<Reserva> crear(@RequestBody ReservaRequest request) {
        Reserva creada = reservaUseCase.crear(mapperReserva.toReserva(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/buscar/{idReserva}")
    public ResponseEntity<Reserva> buscar(@PathVariable Long idReserva) {
        return ResponseEntity.ok(reservaUseCase.buscar(idReserva));
    }

    // ------------------- APIs de negocio (no CRUD) -------------------

    // 4. Historial de reservas de un usuario
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Reserva>> historial(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(reservaUseCase.listarPorUsuario(idUsuario));
    }

    // 5. Confirmar una reserva pendiente
    @PatchMapping("/{idReserva}/confirmar")
    public ResponseEntity<Reserva> confirmar(@PathVariable Long idReserva) {
        return ResponseEntity.ok(reservaUseCase.confirmar(idReserva));
    }

    // 6. Cancelar una reserva pendiente o confirmada
    @PatchMapping("/{idReserva}/cancelar")
    public ResponseEntity<Reserva> cancelar(@PathVariable Long idReserva) {
        return ResponseEntity.ok(reservaUseCase.cancelar(idReserva));
    }

    // 7. Finalizar una reserva confirmada
    @PatchMapping("/{idReserva}/finalizar")
    public ResponseEntity<Reserva> finalizar(@PathVariable Long idReserva) {
        return ResponseEntity.ok(reservaUseCase.finalizar(idReserva));
    }

    // 8. ¿Una fecha es festivo? Consulta la API externa: ?fecha=2026-12-25
    @GetMapping("/festivo")
    public ResponseEntity<Map<String, Object>> festivo(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("fecha", fecha.toString());
        cuerpo.put("festivo", reservaUseCase.consultarFestivo(fecha));
        return ResponseEntity.ok(cuerpo);
    }
}
