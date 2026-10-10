package com.iiss.sensores.grupo_2.controlador.shared;

public interface TemperatureEngineService {
    void iniciar();
    void detener();
    boolean isActivo();
}
