package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.DiasPunta;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuntaTest {
    @Test
    void esHorarioPunta_devuelveTrueEnHorarioDeDiaHabil() {
        Punta punta = new Punta("17:00", "23:00", DiasPunta.HABILES);

        assertTrue(punta.esHorarioPunta(ZonedDateTime.parse("2026-10-07T18:00:00Z")));
    }

    @Test
    void esHorarioPunta_devuelveFalseEnFinDeSemana() {
        Punta punta = new Punta("17:00", "23:00", DiasPunta.HABILES);

        assertFalse(punta.esHorarioPunta(ZonedDateTime.parse("2026-10-10T18:00:00Z")));
    }

    @Test
    void esHorarioPunta_devuelveFalseSiLaFechaHoraEsNula() {
        Punta punta = new Punta("17:00", "23:00", DiasPunta.HABILES);

        assertFalse(punta.esHorarioPunta(null));
    }
}
