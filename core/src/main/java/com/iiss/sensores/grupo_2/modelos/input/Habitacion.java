package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;

public record Habitacion(
        String id,
        String name,
        double expectedTemperature,
        double powerKW,
        String idThermostat,
        String topicThermostat,
        String idSwitch,
        String urlSwitch,

        Double currentTemperatureC,
//        double currentTemperatureF,
        EstadoSwitch currentSwitchState
) {}