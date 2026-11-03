package com.iiss.sensores.grupo_2.modelos.input;

import java.time.ZonedDateTime;

/** Representa la tarifa del sitio y su franja de hora punta. */
public record Fee(RushHour rushHour) {
  /** Indica si la fecha y hora dadas están dentro de la franja configurada. */
  public boolean isRushHour(ZonedDateTime dateTime) {
    return rushHour != null && rushHour.isRushHour(dateTime);
  }
}
