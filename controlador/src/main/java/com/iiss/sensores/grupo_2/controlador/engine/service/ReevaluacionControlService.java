package com.iiss.sensores.grupo_2.controlador.engine.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReevaluacionControlService {
    private final ControllerTempAuto controllerTempAuto;

    public ReevaluacionControlService(ControllerTempAuto controllerTempAuto) {
        this.controllerTempAuto = controllerTempAuto;
    }

    @Scheduled(fixedDelayString = "${controlador.reevaluacion-ms:5000}")
    public void reevaluar() {
        if (controllerTempAuto.isActive()) {
            controllerTempAuto.execute();
        }
    }
}
