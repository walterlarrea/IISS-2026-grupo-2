package com.iiss.sensores.grupo_2.controlador.engine.model;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;

import java.time.LocalDate;

public record SwitchEstadoResponse(
        String idSwitch,
        EstadoSwitch estado,
        LocalDate aplicadoEn
) {
}
