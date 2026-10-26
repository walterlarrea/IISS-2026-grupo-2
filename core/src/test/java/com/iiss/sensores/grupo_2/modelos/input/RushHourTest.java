package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.RushDays;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RushHourTest {
    @Test
    void esHorarioPunta_devuelveTrueEnHorarioDeDiaHabil() {
        RushHour rushHour = new RushHour("17:00", "23:00", RushDays.HABILES);

        assertTrue(rushHour.esHorarioPunta(ZonedDateTime.parse("2026-10-07T18:00:00Z")));
    }

    @Test
    void esHorarioPunta_devuelveFalseEnFinDeSemana() {
        RushHour rushHour = new RushHour("17:00", "23:00", RushDays.HABILES);

        assertFalse(rushHour.esHorarioPunta(ZonedDateTime.parse("2026-10-10T18:00:00Z")));
    }

    @Test
    void esHorarioPunta_devuelveFalseSiLaFechaHoraEsNula() {
        RushHour rushHour = new RushHour("17:00", "23:00", RushDays.HABILES);

        assertFalse(rushHour.esHorarioPunta(null));
    }
}
