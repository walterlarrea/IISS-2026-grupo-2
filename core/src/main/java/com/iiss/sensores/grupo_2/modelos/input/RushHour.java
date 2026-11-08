package com.iiss.sensores.grupo_2.modelos.input;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.iiss.sensores.grupo_2.enums.RushDays;

public record RushHour(String fromTime, String toTime, RushDays dias) {
  /**
   * Most recent moment (at or before the given time) at which a rush hour period ended, or null if
   * there is none in the last week. Weekends are skipped for {@code HABILES}.
   */
  public ZonedDateTime lastRushHourEnd(ZonedDateTime currentDateTime) {
    if (currentDateTime == null || this.dias == null || this.toTime == null) {
      return null;
    }
    LocalTime end = LocalTime.parse(this.toTime);
    for (int daysBack = 0; daysBack <= 7; daysBack++) {
      ZonedDateTime candidate =
          currentDateTime.toLocalDate().minusDays(daysBack).atTime(end).atZone(currentDateTime.getZone());
      boolean weekend =
          candidate.getDayOfWeek() == DayOfWeek.SATURDAY
              || candidate.getDayOfWeek() == DayOfWeek.SUNDAY;
      if (candidate.isAfter(currentDateTime) || (this.dias == RushDays.HABILES && weekend)) {
        continue;
      }
      return candidate;
    }
    return null;
  }

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
