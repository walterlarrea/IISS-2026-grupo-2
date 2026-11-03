package com.iiss.sensores.grupo_2.switches.dto;

import com.iiss.sensores.grupo_2.switches.EstadoSwitch;

import java.time.Instant;

/** Respuesta de un comando que también indica si el estado cambió. */
public class SwitchAccionResponse extends SwitchResponse {
  private final boolean cambio;

  /** Crea la respuesta del comando con el resultado del cambio de estado. */
  public SwitchAccionResponse(
      String idSwitch, EstadoSwitch estado, Instant aplicadoEn, boolean cambio) {
    super(idSwitch, estado, aplicadoEn);
    this.cambio = cambio;
  }

  /** Indica si el comando modificó el estado que tenía el switch. */
  public boolean isCambio() {
    return cambio;
  }
}
