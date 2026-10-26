package com.iiss.sensores.grupo_2.switches.dto;

import com.iiss.sensores.grupo_2.switches.EstadoSwitch;

import java.time.Instant;

public class SwitchAccionResponse extends SwitchResponse {
    private final boolean cambio;

    public SwitchAccionResponse(String idSwitch, EstadoSwitch estado, Instant aplicadoEn, boolean cambio){
        super(idSwitch, estado, aplicadoEn);
        this.cambio = cambio;
    }

    public boolean isCambio() {
        return cambio;
    }
}
