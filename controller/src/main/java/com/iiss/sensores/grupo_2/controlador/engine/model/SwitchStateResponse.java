package com.iiss.sensores.grupo_2.controlador.engine.model;

import com.iiss.sensores.grupo_2.enums.SwitchState;

import java.time.LocalDate;

public record SwitchStateResponse(
        String idSwitch,
        SwitchState estado,
        LocalDate aplicadoEn
) {
}
