package com.reservaslab.reservas.infraestructure.entry_points;

import com.reservaslab.reservas.domain.model.Disponibilidad;
import com.reservaslab.reservas.domain.model.Laboratorio;
import com.reservaslab.reservas.domain.usecase.LaboratorioUseCase;
import com.reservaslab.reservas.infraestructure.entry_points.dto.EstadoLaboratorioRequest;
import com.reservaslab.reservas.infraestructure.entry_points.dto.LaboratorioRequest;
import com.reservaslab.reservas.infraestructure.mapper.MapperLaboratorio;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/laboratorios")
@RequiredArgsConstructor
public class LaboratorioController {

    private final LaboratorioUseCase laboratorioUseCase;
    private final MapperLaboratorio mapperLaboratorio;

    // ------------------------- CRUD -------------------------

    @PostMapping("/crear")
    public ResponseEntity<Laboratorio> crear(@RequestBody LaboratorioRequest request) {
        Laboratorio creado = laboratorioUseCase.crear(mapperLaboratorio.toLaboratorio(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/lista")
    public ResponseEntity<List<Laboratorio>> listar() {
        return ResponseEntity.ok(laboratorioUseCase.listar());
    }

    @GetMapping("/buscar/{idLaboratorio}")
    public ResponseEntity<Laboratorio> buscar(@PathVariable Long idLaboratorio) {
        return ResponseEntity.ok(laboratorioUseCase.buscar(idLaboratorio));
    }

    @PutMapping("/actualizar/{idLaboratorio}")
    public ResponseEntity<Laboratorio> actualizar(@PathVariable Long idLaboratorio,
                                                  @RequestBody LaboratorioRequest request) {
        return ResponseEntity.ok(
                laboratorioUseCase.actualizar(idLaboratorio, mapperLaboratorio.toLaboratorio(request)));
    }

    @DeleteMapping("/eliminar/{idLaboratorio}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long idLaboratorio) {
        laboratorioUseCase.eliminar(idLaboratorio);
        return ResponseEntity.ok(Map.of("mensaje", "Laboratorio eliminado correctamente"));
    }

    // ------------------- APIs de negocio (no CRUD) -------------------

    // 1. Poner en mantenimiento o volver a habilitar
    @PatchMapping("/{idLaboratorio}/estado")
    public ResponseEntity<Laboratorio> cambiarEstado(@PathVariable Long idLaboratorio,
                                                     @RequestBody EstadoLaboratorioRequest request) {
        return ResponseEntity.ok(laboratorioUseCase.cambiarEstado(idLaboratorio, request.estado()));
    }

    // 2. Agenda del laboratorio en un día, hora por hora: ?fecha=2026-11-20
    @GetMapping("/{idLaboratorio}/disponibilidad")
    public ResponseEntity<Disponibilidad> disponibilidad(
            @PathVariable Long idLaboratorio,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(laboratorioUseCase.consultarDisponibilidad(idLaboratorio, fecha));
    }

    // 3. Laboratorios libres en un horario: ?fecha=2026-11-20&horaInicio=08:00&horaFin=10:00
    @GetMapping("/disponibles")
    public ResponseEntity<List<Laboratorio>> disponibles(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("horaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam("horaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {
        return ResponseEntity.ok(laboratorioUseCase.listarDisponibles(fecha, horaInicio, horaFin));
    }
}
