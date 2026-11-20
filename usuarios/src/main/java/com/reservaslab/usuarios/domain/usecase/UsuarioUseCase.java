package com.reservaslab.usuarios.domain.usecase;

import com.reservaslab.usuarios.domain.Excepciones.AccesoDenegadoException;
import com.reservaslab.usuarios.domain.Excepciones.ConflictoException;
import com.reservaslab.usuarios.domain.Excepciones.CredencialesInvalidasException;
import com.reservaslab.usuarios.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.usuarios.domain.model.EstadisticasUsuarios;
import com.reservaslab.usuarios.domain.model.Rol;
import com.reservaslab.usuarios.domain.model.Usuario;
import com.reservaslab.usuarios.domain.model.gateway.EncriptarGateway;
import com.reservaslab.usuarios.domain.model.gateway.UsuarioGateway;
import lombok.RequiredArgsConstructor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class UsuarioUseCase {

    private static final String FORMATO_CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final int LARGO_MINIMO_CLAVE = 6;

    private final UsuarioGateway usuarioGateway;
    private final EncriptarGateway encriptarGateway;

    // ---------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------

    public Usuario registrar(Usuario usuario) {
        Usuario nuevo = Optional.ofNullable(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Los datos del usuario son obligatorios"));

        String nombre = validarTexto(nuevo.getNombre(), "El nombre es obligatorio");
        String correo = validarCorreo(nuevo.getCorreo());
        validarClave(nuevo.getClave());
        validarRol(nuevo.getRol());
        exigirCorreoLibre(correo, null);

        nuevo.setIdUsuario(null);                 // crear siempre crea
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        nuevo.setClave(encriptarGateway.encriptar(nuevo.getClave()));
        nuevo.setActivo(true);                    // toda cuenta nueva empieza activa
        return usuarioGateway.guardar(nuevo);
    }

    public Usuario buscar(Long idUsuario) {
        Long idValidado = validarId(idUsuario);
        return usuarioGateway.buscarPorId(idValidado)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el id: " + idValidado));
    }

    public List<Usuario> listar() {
        return usuarioGateway.listar();
    }

    /** Cambia nombre, correo y rol. La clave y el estado tienen sus propias operaciones. */
    public Usuario actualizar(Long idUsuario, Usuario datos) {
        Usuario existente = buscar(idUsuario);
        Usuario cambios = Optional.ofNullable(datos)
                .orElseThrow(() -> new IllegalArgumentException("Los datos del usuario son obligatorios"));

        String nombre = validarTexto(cambios.getNombre(), "El nombre es obligatorio");
        String correo = validarCorreo(cambios.getCorreo());
        validarRol(cambios.getRol());
        exigirCorreoLibre(correo, existente.getIdUsuario());

        existente.setNombre(nombre);
        existente.setCorreo(correo);
        existente.setRol(cambios.getRol());
        return usuarioGateway.guardar(existente);
    }

    public void eliminar(Long idUsuario) {
        Usuario existente = buscar(idUsuario);
        usuarioGateway.eliminar(existente.getIdUsuario());
    }

    // ---------------------------------------------------------------
    // Operaciones de negocio (no CRUD)
    // ---------------------------------------------------------------

    public Usuario login(String correo, String clave) {
        String correoValidado = validarTexto(correo, "El correo es obligatorio").toLowerCase();
        String claveValidada = validarTexto(clave, "La clave es obligatoria");

        // Mismo mensaje si falla el correo o la clave: no se revela cuál de los dos existe
        Usuario usuario = usuarioGateway.buscarPorCorreo(correoValidado)
                .filter(encontrado -> encriptarGateway.verificar(claveValidada, encontrado.getClave()))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o clave incorrectos"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AccesoDenegadoException("La cuenta está desactivada. Contacta al administrador");
        }
        return usuario;
    }

    public Usuario cambiarClave(Long idUsuario, String claveActual, String claveNueva) {
        Usuario usuario = buscar(idUsuario);
        String actual = validarTexto(claveActual, "La clave actual es obligatoria");

        if (!encriptarGateway.verificar(actual, usuario.getClave())) {
            throw new CredencialesInvalidasException("La clave actual no es correcta");
        }
        validarClave(claveNueva);
        if (encriptarGateway.verificar(claveNueva, usuario.getClave())) {
            throw new IllegalArgumentException("La clave nueva debe ser diferente a la actual");
        }

        usuario.setClave(encriptarGateway.encriptar(claveNueva));
        return usuarioGateway.guardar(usuario);
    }

    public Usuario desactivar(Long idUsuario) {
        return cambiarEstado(idUsuario, false);
    }

    public Usuario activar(Long idUsuario) {
        return cambiarEstado(idUsuario, true);
    }

    public EstadisticasUsuarios estadisticas() {
        List<Usuario> usuarios = usuarioGateway.listar();

        Map<Rol, Long> porRol = new EnumMap<>(Rol.class);
        for (Rol rol : Rol.values()) {
            porRol.put(rol, 0L);
        }
        usuarios.forEach(usuario -> porRol.merge(usuario.getRol(), 1L, Long::sum));

        long activos = usuarios.stream()
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                .count();
        return new EstadisticasUsuarios(usuarios.size(), activos, usuarios.size() - activos, porRol);
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    private Usuario cambiarEstado(Long idUsuario, boolean activo) {
        Usuario usuario = buscar(idUsuario);
        if (Boolean.valueOf(activo).equals(usuario.getActivo())) {
            throw new ConflictoException("La cuenta ya está " + (activo ? "activa" : "desactivada"));
        }
        usuario.setActivo(activo);
        return usuarioGateway.guardar(usuario);
    }

    private Long validarId(Long id) {
        return Optional.ofNullable(id)
                .filter(valor -> valor > 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El id del usuario es obligatorio y debe ser mayor a cero"));
    }

    private String validarTexto(String texto, String mensajeError) {
        return Optional.ofNullable(texto)
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException(mensajeError));
    }

    private String validarCorreo(String correo) {
        return Optional.of(validarTexto(correo, "El correo es obligatorio").toLowerCase())
                .filter(valor -> valor.matches(FORMATO_CORREO))
                .orElseThrow(() -> new IllegalArgumentException("El correo no tiene un formato válido"));
    }

    private void validarClave(String clave) {
        Optional.ofNullable(clave)
                .filter(valor -> valor.trim().length() >= LARGO_MINIMO_CLAVE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La clave es obligatoria y debe tener mínimo " + LARGO_MINIMO_CLAVE + " caracteres"));
    }

    private void validarRol(Rol rol) {
        Optional.ofNullable(rol)
                .orElseThrow(() -> new IllegalArgumentException("El rol es obligatorio: ESTUDIANTE, DOCENTE o ADMIN"));
    }

    private void exigirCorreoLibre(String correo, Long idPropio) {
        usuarioGateway.buscarPorCorreo(correo)
                .filter(otro -> !otro.getIdUsuario().equals(idPropio))
                .ifPresent(otro -> {
                    throw new ConflictoException("Ya existe un usuario con el correo " + correo);
                });
    }
}
