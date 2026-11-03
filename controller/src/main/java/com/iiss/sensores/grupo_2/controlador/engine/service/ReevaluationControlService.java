package com.iiss.sensores.grupo_2.controlador.engine.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Programa ciclos periódicos para reevaluar las decisiones de control. */
@Component
public class ReevaluationControlService {
  private final TemperatureControlEngine temperatureControlEngine;

  /** Crea el planificador vinculado al motor de control. */
  public ReevaluationControlService(TemperatureControlEngine temperatureControlEngine) {
    this.temperatureControlEngine = temperatureControlEngine;
  }

  /** Ejecuta una reevaluación si el controlador está activo. */
  @Scheduled(fixedDelayString = "${controlador.reevaluacion-ms:5000}")
  public void reevaluar() {
    if (temperatureControlEngine.isActive()) {
      temperatureControlEngine.execute();
    }
  }
}
