package com.iiss.sensores.grupo_2.controlador.api.model;

import java.time.Instant;

/** Respuesta con el estado operativo y los datos del reloj del controlador. */
public record StateControl(
    Estado estado, Instant fechaHora, Integer factorTiempo, Instant iniciadoEn) {
  /** Estados posibles del motor de control. */
  public enum Estado {
    EN_EJECUCION,
    DETENIDO
  }
}
