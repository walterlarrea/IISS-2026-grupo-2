package com.iiss.sensores.grupo_2.controlador.shared;

import com.iiss.sensores.grupo_2.controlador.api.model.EstadoControl;

import java.time.Instant;

public interface TemperatureEngineService {
    void iniciar();
    void iniciar(Instant fechaHora, double factorTiempo);
    void detener();
    boolean isActivo();
    EstadoControl obtenerEstado();
}
