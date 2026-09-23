package org.switches.dto;

import jakarta.validation.constraints.NotNull;

public class SwitchRequest {

    @NotNull
    private Boolean encendido;

    public Boolean getEncendido() {
        return encendido;
    }

    public void setEncendido(Boolean encendido) {
        this.encendido = encendido;
    }
}