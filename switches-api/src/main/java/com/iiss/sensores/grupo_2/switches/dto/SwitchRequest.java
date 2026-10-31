package com.iiss.sensores.grupo_2.switches.dto;

import com.iiss.sensores.grupo_2.switches.EstadoSwitch;

import jakarta.validation.constraints.NotNull;

public class SwitchRequest {

  @NotNull private EstadoSwitch accion;

  public EstadoSwitch getAccion() {
    return accion;
  }
}
