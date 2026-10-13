package com.iiss.sensores.grupo_2.controlador.engine.client;

import java.time.Instant;

public record SwitchResponse(String idSwitch, String estado, boolean cambio, Instant aplicadoEn) {
}
