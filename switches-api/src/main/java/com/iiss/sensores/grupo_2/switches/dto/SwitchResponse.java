package com.iiss.sensores.grupo_2.switches.dto;

import com.iiss.sensores.grupo_2.switches.EstadoSwitch;

import java.time.Instant;

public class SwitchResponse {
    private final String idSwitch;
    private final EstadoSwitch estado;
    private final Instant aplicadoEn;

    public SwitchResponse(String idSwitch, EstadoSwitch estado, Instant aplicadoEn){
        this.idSwitch = idSwitch;
        this.estado = estado;
        this.aplicadoEn = aplicadoEn;
    }

    public String getIdSwitch() {
        return idSwitch;
    }

    public EstadoSwitch getEstado() {
        return estado;
    }

    public Instant getAplicadoEn() {
        return aplicadoEn;
    }
}