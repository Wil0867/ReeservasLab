package com.reservaslab.reservas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReglasReservaTest {

    @Test
    void aceptaUnaFranjaValida() {
        assertDoesNotThrow(() -> ReglasReserva.validarFranja(LocalTime.of(7, 0), LocalTime.of(11, 0)));
        assertDoesNotThrow(() -> ReglasReserva.validarFranja(LocalTime.of(19, 0), LocalTime.of(21, 0)));
    }

    @Test
    void rechazaHorasNulas() {
        assertThrows(IllegalArgumentException.class, () -> ReglasReserva.validarFranja(null, LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> ReglasReserva.validarFranja(LocalTime.of(9, 0), null));
    }

    @Test
    void rechazaInicioPosteriorOIgualAlFin() {
        assertThrows(IllegalArgumentException.class,
                () -> ReglasReserva.validarFranja(LocalTime.of(10, 0), LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> ReglasReserva.validarFranja(LocalTime.of(10, 0), LocalTime.of(10, 0)));
    }

    @Test
    void rechazaFueraDelHorarioDeAtencion() {
        assertThrows(IllegalArgumentException.class,
                () -> ReglasReserva.validarFranja(LocalTime.of(6, 0), LocalTime.of(8, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> ReglasReserva.validarFranja(LocalTime.of(20, 0), LocalTime.of(22, 0)));
    }

    @Test
    void rechazaMasDeCuatroHoras() {
        assertThrows(IllegalArgumentException.class,
                () -> ReglasReserva.validarFranja(LocalTime.of(8, 0), LocalTime.of(13, 0)));
    }
}
