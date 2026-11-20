package com.reservaslab.usuarios.infraestructure.entry_points;

import com.reservaslab.usuarios.domain.model.EstadisticasUsuarios;
import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.domain.usecase.UsuarioUseCase;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.ActualizarRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.CambioClaveRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.LoginRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.RegistroRequest;
import com.reservaslab.usuarios.infraestructure.entry_points.dto.UsuarioResponse;
import com.reservaslab.usuarios.infraestructure.mapper.MapperUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sin try/catch: los errores los traduce ManejadorGlobalExcepciones. */
@RestController
@RequestMapping("api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioUseCase usuarioUseCase;
    private final MapperUsuario mapperUsuario;

    // ------------------------- CRUD -------------------------

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody RegistroRequest request) {
        Usuario creado = usuarioUseCase.registrar(mapperUsuario.toUsuario(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapperUsuario.toResponse(creado));
    }

    @GetMapping("/lista")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioUseCase.listar().stream().map(mapperUsuario::toResponse).toList());
    }

    @GetMapping("/buscar/{idUsuario}")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(mapperUsuario.toResponse(usuarioUseCase.buscar(idUsuario)));
    }

    @PutMapping("/actualizar/{idUsuario}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long idUsuario,
                                                      @RequestBody ActualizarRequest request) {
        Usuario actualizado = usuarioUseCase.actualizar(idUsuario, mapperUsuario.toUsuario(request));
        return ResponseEntity.ok(mapperUsuario.toResponse(actualizado));
    }

    @DeleteMapping("/eliminar/{idUsuario}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long idUsuario) {
        usuarioUseCase.eliminar(idUsuario);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
    }

    // ------------------- APIs de negocio (no CRUD) -------------------

    // 1. Iniciar sesión
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioUseCase.login(request.correo(), request.clave());
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Bienvenido, " + usuario.getNombre());
        cuerpo.put("usuario", mapperUsuario.toResponse(usuario));
        return ResponseEntity.ok(cuerpo);
    }

    // 2. Cambiar la clave (exige la clave actual)
    @PatchMapping("/{idUsuario}/clave")
    public ResponseEntity<Map<String, Object>> cambiarClave(@PathVariable Long idUsuario,
                                                            @RequestBody CambioClaveRequest request) {
        usuarioUseCase.cambiarClave(idUsuario, request.claveActual(), request.claveNueva());
        return ResponseEntity.ok(Map.of("mensaje", "Clave actualizada correctamente"));
    }

    // 3. Desactivar una cuenta (no podrá iniciar sesión ni reservar)
    @PatchMapping("/{idUsuario}/desactivar")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(mapperUsuario.toResponse(usuarioUseCase.desactivar(idUsuario)));
    }

    // 4. Reactivar una cuenta
    @PatchMapping("/{idUsuario}/activar")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(mapperUsuario.toResponse(usuarioUseCase.activar(idUsuario)));
    }

    // 5. Validar un usuario: lo consumen Reservas y Notificaciones
    @GetMapping("/{idUsuario}/validar")
    public ResponseEntity<UsuarioResponse> validar(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(mapperUsuario.toResponse(usuarioUseCase.buscar(idUsuario)));
    }

    // 6. Estadísticas: totales, activos y cantidad por rol
    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasUsuarios> estadisticas() {
        return ResponseEntity.ok(usuarioUseCase.estadisticas());
    }
}
