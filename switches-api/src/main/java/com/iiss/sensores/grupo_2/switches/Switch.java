package com.iiss.sensores.grupo_2.switches;

import java.time.LocalDate;

public class Switch {

    private final String id;
    private EstadoSwitch estado;
    private LocalDate aplicadoEn;

    public Switch(String id, EstadoSwitch estado, LocalDate aplicadoEn) {
        this.id = id;
        this.estado = estado;
        this.aplicadoEn = aplicadoEn;
    }

    public String getId() {
        return id;
    }

    public EstadoSwitch getEstado() {
        return estado;
    }

    public void setEstado(EstadoSwitch estado) {
        this.estado = estado;
    }

    public LocalDate getAplicadoEn() {
        return aplicadoEn;
    }

    public void setAplicadoEn(LocalDate aplicadoEn) {
        this.aplicadoEn = aplicadoEn;
    }
}