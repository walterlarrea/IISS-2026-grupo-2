package com.iiss.sensores.grupo_2.controlador.shared;

import com.iiss.sensores.grupo_2.controlador.api.model.EstadoControl;

import java.time.Instant;

public interface TemperatureEngineService {
    void start(Instant fechaHora, int timeFactor);
    void stop();
    boolean isActive();
    EstadoControl getTempControllerStatus();
}
