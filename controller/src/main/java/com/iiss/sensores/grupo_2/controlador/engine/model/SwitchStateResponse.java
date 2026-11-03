package com.iiss.sensores.grupo_2.controlador.engine.model;

import com.iiss.sensores.grupo_2.enums.SwitchState;

import java.time.Instant;

/** Estado de un switch devuelto por la API remota. */
public record SwitchStateResponse(String idSwitch, SwitchState estado, Instant aplicadoEn) {}
