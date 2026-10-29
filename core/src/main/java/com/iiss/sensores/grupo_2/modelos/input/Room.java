package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.SwitchState;

public record Room(
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
    SwitchState currentSwitchState) {}
