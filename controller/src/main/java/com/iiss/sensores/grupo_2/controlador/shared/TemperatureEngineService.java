package com.iiss.sensores.grupo_2.controlador.shared;

import com.iiss.sensores.grupo_2.controlador.api.model.StateControl;

import java.time.Instant;

/** Contrato para iniciar, detener y consultar el motor de control de temperatura. */
public interface TemperatureEngineService {
  /** Inicia el motor desde la fecha indicada con el factor temporal dado. */
  void start(Instant fechaHora, int timeFactor);

  /** Detiene el motor. */
  void stop();

  /** Indica si el motor está en ejecución. */
  boolean isActive();

  /** Devuelve el estado actual del motor. */
  StateControl getTempControllerStatus();
}
