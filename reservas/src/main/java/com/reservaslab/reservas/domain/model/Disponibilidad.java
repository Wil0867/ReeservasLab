package com.reservaslab.reservas.domain.model;

import java.time.LocalDate;
import java.util.List;

/** Agenda de un laboratorio para un día, hora por hora. */
public record Disponibilidad(Long idLaboratorio, LocalDate fecha, boolean festivo,
                             boolean laboratorioHabilitado, List<Franja> franjas) {
}
