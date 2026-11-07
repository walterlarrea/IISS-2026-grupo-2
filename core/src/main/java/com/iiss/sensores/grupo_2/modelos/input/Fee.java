package com.iiss.sensores.grupo_2.modelos.input;

import java.time.ZonedDateTime;

public record Fee(RushHour rushHour) {
  /** Most recent end of a rush hour period at or before {@code dateTime}, or null if unknown */
  public ZonedDateTime lastRushHourEnd(ZonedDateTime dateTime) {
    return rushHour == null ? null : rushHour.lastRushHourEnd(dateTime);
  }

  public boolean isRushHour(ZonedDateTime dateTime) {
    return rushHour != null && rushHour.isRushHour(dateTime);
  }
}
