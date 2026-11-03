package com.iiss.sensores.grupo_2.switches.dto;

import com.iiss.sensores.grupo_2.switches.EstadoSwitch;

import jakarta.validation.constraints.NotNull;

/** Representa la acción solicitada para un switch. */
public class SwitchRequest {

  @NotNull private EstadoSwitch accion;

  /** Devuelve la acción solicitada. */
  public EstadoSwitch getAccion() {
    return accion;
  }
}
