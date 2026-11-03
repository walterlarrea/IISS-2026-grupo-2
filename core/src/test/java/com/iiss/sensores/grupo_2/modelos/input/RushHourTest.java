package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.RushDays;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Comprueba la detección de los intervalos de hora punta. */
class RushHourTest {
  /** Confirma que una hora hábil dentro del intervalo se considera hora punta. */
  @Test
  void esHorarioPunta_devuelveTrueEnHorarioDeDiaHabil() {
    RushHour rushHour = new RushHour("17:00", "23:00", RushDays.HABILES);

    assertTrue(rushHour.isRushHour(ZonedDateTime.parse("2026-10-07T18:00:00Z")));
  }

  /** Confirma que los fines de semana no aplican una tarifa de días hábiles. */
  @Test
  void esHorarioPunta_devuelveFalseEnFinDeSemana() {
    RushHour rushHour = new RushHour("17:00", "23:00", RushDays.HABILES);

    assertFalse(rushHour.isRushHour(ZonedDateTime.parse("2026-10-10T18:00:00Z")));
  }

  /** Confirma que una fecha/hora nula no se considera hora punta. */
  @Test
  void esHorarioPunta_devuelveFalseSiLaFechaHoraEsNula() {
    RushHour rushHour = new RushHour("17:00", "23:00", RushDays.HABILES);

    assertFalse(rushHour.isRushHour(null));
  }
}
