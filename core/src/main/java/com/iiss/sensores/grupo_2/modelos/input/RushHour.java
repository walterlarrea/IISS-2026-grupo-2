package com.iiss.sensores.grupo_2.modelos.input;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.iiss.sensores.grupo_2.enums.RushDays;

/** Define el horario y los días en que se considera activa la hora punta. */
public record RushHour(String fromTime, String toTime, RushDays dias) {
  /** Comprueba si la fecha y hora indicadas pertenecen a la franja configurada. */
  public boolean isRushHour(ZonedDateTime currentDateTime) {
    if (currentDateTime == null || this.dias == null) {
      return false;
    }

    switch (this.dias) {
      case RushDays.HABILES:
        DayOfWeek dayOfWeek = currentDateTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
          // No rush hour control on weekends for RushDays == HABILES
          return false;
        }
        break;
      case RushDays.TODOS:
        // Always continue with rush hour verification for RushDays == TODOS
        break;
      default:
        break;
    }

    ZonedDateTime timeStart =
        currentDateTime
            .toInstant()
            .atZone(currentDateTime.getOffset())
            .with(LocalTime.parse(this.fromTime));
    ZonedDateTime timeEnd =
        currentDateTime
            .toInstant()
            .atZone(currentDateTime.getOffset())
            .with(LocalTime.parse(this.toTime));

    //    IO.println(currentDateTime + " | Dates " + timeStart + " | " + timeEnd);
    return !currentDateTime.isBefore(timeStart) && !currentDateTime.isAfter(timeEnd);
  }
}
