package com.reservaslab.notificaciones.domain.usecase;

import com.reservaslab.notificaciones.domain.Excepciones.RecursoNoEncontradoException;
import com.reservaslab.notificaciones.domain.Excepciones.ReglaDeNegocioException;
import com.reservaslab.notificaciones.domain.Excepciones.ServicioExternoException;
import com.reservaslab.notificaciones.domain.model.EstadisticasNotificaciones;
import com.reservaslab.notificaciones.domain.model.EstadoNotificacion;
import com.reservaslab.notificaciones.domain.model.Notificacion;
import com.reservaslab.notificaciones.domain.model.TipoNotificacion;
import com.reservaslab.notificaciones.domain.model.gateway.CorreoGateway;
import com.reservaslab.notificaciones.domain.model.gateway.NotificacionGateway;
import com.reservaslab.notificaciones.domain.model.gateway.UsuarioGateway;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class NotificacionUseCase {

    public static final int MAXIMO_INTENTOS = 3;
    private static final int LARGO_MAXIMO_ERROR = 500;
    private static final Pattern FORMATO_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final NotificacionGateway notificacionGateway;
    private final CorreoGateway correoGateway;
    private final UsuarioGateway usuarioGateway;
    private final Clock clock;

    // ---------------------------------------------------------------
    // Enviar
    // ---------------------------------------------------------------

    /**
     * Envía el correo y SIEMPRE deja registro: ENVIADA si salió, FALLIDA si el proveedor lo rechazó.
     * Si no llega destinatario, se usa el correo que el usuario tiene registrado en Usuarios.
     */
    public Notificacion enviar(Notificacion solicitud) {
        Notificacion nueva = Optional.ofNullable(solicitud)
                .orElseThrow(() -> new IllegalArgumentException("Los datos de la notificación son obligatorios"));

        Long idUsuario = validarId(nueva.getIdUsuario(), "usuario");
        String asunto = textoObligatorio(nueva.getAsunto(), "El asunto es obligatorio");
        String mensaje = textoObligatorio(nueva.getMensaje(), "El mensaje es obligatorio");
        String destinatario = resolverDestinatario(idUsuario, nueva.getDestinatario());

        nueva.setIdNotificacion(null);
        nueva.setAsunto(asunto);
        nueva.setMensaje(mensaje);
        nueva.setDestinatario(destinatario);
        nueva.setTipo(Optional.ofNullable(nueva.getTipo()).orElse(TipoNotificacion.GENERAL));
        nueva.setLeida(false);
        nueva.setIntentos(0);
        nueva.setFechaCreacion(LocalDateTime.now(clock));

        intentarEnvio(nueva);
        return notificacionGateway.guardar(nueva);
    }

    /** Vuelve a enviar una notificación FALLIDA, hasta completar el máximo de intentos. */
    public Notificacion reintentar(Long idNotificacion) {
        Notificacion notificacion = buscar(idNotificacion);
        if (notificacion.getEstado() != EstadoNotificacion.FALLIDA) {
            throw new ReglaDeNegocioException("Solo se pueden reintentar las notificaciones fallidas");
        }
        if (notificacion.getIntentos() >= MAXIMO_INTENTOS) {
            throw new ReglaDeNegocioException(
                    "La notificación ya alcanzó el máximo de " + MAXIMO_INTENTOS + " intentos");
        }
        intentarEnvio(notificacion);
        return notificacionGateway.guardar(notificacion);
    }

    // ---------------------------------------------------------------
    // Consultas y cambios
    // ---------------------------------------------------------------

    public Notificacion buscar(Long idNotificacion) {
        Long idValidado = validarId(idNotificacion, "notificación");
        return notificacionGateway.buscarPorId(idValidado)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una notificación con el id: " + idValidado));
    }

    public List<Notificacion> listar() {
        return notificacionGateway.listar();
    }

    public void eliminar(Long idNotificacion) {
        notificacionGateway.eliminar(buscar(idNotificacion).getIdNotificacion());
    }

    public Notificacion marcarLeida(Long idNotificacion) {
        Notificacion notificacion = buscar(idNotificacion);
        if (Boolean.TRUE.equals(notificacion.getLeida())) {
            throw new ReglaDeNegocioException("La notificación ya estaba marcada como leída");
        }
        notificacion.setLeida(true);
        return notificacionGateway.guardar(notificacion);
    }

    public List<Notificacion> listarPorUsuario(Long idUsuario) {
        return notificacionGateway.listarPorUsuario(validarId(idUsuario, "usuario"));
    }

    public List<Notificacion> listarNoLeidas(Long idUsuario) {
        return notificacionGateway.listarNoLeidasPorUsuario(validarId(idUsuario, "usuario"));
    }

    public List<Notificacion> listarFallidas() {
        return notificacionGateway.listarPorEstado(EstadoNotificacion.FALLIDA);
    }

    public EstadisticasNotificaciones estadisticas() {
        List<Notificacion> todas = notificacionGateway.listar();
        Map<TipoNotificacion, Long> porTipo = new EnumMap<>(TipoNotificacion.class);
        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            porTipo.put(tipo, todas.stream().filter(notificacion -> notificacion.getTipo() == tipo).count());
        }
        long enviadas = todas.stream()
                .filter(notificacion -> notificacion.getEstado() == EstadoNotificacion.ENVIADA).count();
        long noLeidas = todas.stream()
                .filter(notificacion -> !Boolean.TRUE.equals(notificacion.getLeida())).count();
        return new EstadisticasNotificaciones(todas.size(), enviadas, todas.size() - enviadas, noLeidas, porTipo);
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    /** Un intento de envío: suma el intento y deja el estado según el resultado. */
    private void intentarEnvio(Notificacion notificacion) {
        notificacion.setIntentos(notificacion.getIntentos() + 1);
        try {
            correoGateway.enviar(notificacion.getDestinatario(), notificacion.getAsunto(), notificacion.getMensaje());
            notificacion.setEstado(EstadoNotificacion.ENVIADA);
            notificacion.setDetalleError(null);
        } catch (ServicioExternoException e) {
            notificacion.setEstado(EstadoNotificacion.FALLIDA);
            notificacion.setDetalleError(recortar(String.valueOf(e.getMessage())));
        }
    }

    private String resolverDestinatario(Long idUsuario, String destinatario) {
        String correo = Optional.ofNullable(destinatario)
                .map(String::trim)
                .filter(texto -> !texto.isEmpty())
                .orElseGet(() -> usuarioGateway.consultarCorreo(idUsuario)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No existe un usuario con el id: " + idUsuario)));
        if (!FORMATO_CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo del destinatario no es válido: " + correo);
        }
        return correo;
    }

    private Long validarId(Long id, String entidad) {
        return Optional.ofNullable(id)
                .filter(valor -> valor > 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El id de " + entidad + " es obligatorio y debe ser mayor a cero"));
    }

    private String textoObligatorio(String texto, String mensajeError) {
        return Optional.ofNullable(texto)
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException(mensajeError));
    }

    private String recortar(String texto) {
        return texto.length() <= LARGO_MAXIMO_ERROR ? texto : texto.substring(0, LARGO_MAXIMO_ERROR);
    }
}
