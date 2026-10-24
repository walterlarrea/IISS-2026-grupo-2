package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;

public record Habitacion(
        String id,
        String name,
        double expectedTemeprature,
        double powerKW,
        String idTermostat,
        String topicTermostat,
        String idSwitch,
        String urlSwitch,

        double currentTemperatureC,
//        double currentTemperatureF,
        EstadoSwitch currentSwitchState
) {}