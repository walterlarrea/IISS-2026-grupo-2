package com.iiss.sensores.grupo_2.modelos.input;

import java.time.ZonedDateTime;

public record Fee(RushHour rushHour) {
  public boolean isRushHour(ZonedDateTime dateTime) {
    return rushHour != null && rushHour.isRushHour(dateTime);
  }
}
