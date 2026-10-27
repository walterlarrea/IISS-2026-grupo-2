package com.iiss.sensores.grupo_2.controlador.engine.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReevaluationControlService {
  private final TemperatureControlEngine temperatureControlEngine;

  public ReevaluationControlService(TemperatureControlEngine temperatureControlEngine) {
    this.temperatureControlEngine = temperatureControlEngine;
  }

  @Scheduled(fixedDelayString = "${controlador.reevaluacion-ms:5000}")
  public void reevaluar() {
    if (temperatureControlEngine.isActive()) {
      temperatureControlEngine.execute();
    }
  }
}
