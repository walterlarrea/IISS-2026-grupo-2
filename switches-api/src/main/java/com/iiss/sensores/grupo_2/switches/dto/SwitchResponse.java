package com.iiss.sensores.grupo_2.switches.dto;

import com.iiss.sensores.grupo_2.switches.EstadoSwitch;

import java.time.Instant;

/** Respuesta API con el identificador, estado y fecha de aplicación del switch. */
public class SwitchResponse {
  private final String idSwitch;
  private final EstadoSwitch estado;
  private final Instant aplicadoEn;

  /** Crea la respuesta con el estado informado por el servicio. */
  public SwitchResponse(String idSwitch, EstadoSwitch estado, Instant aplicadoEn) {
    this.idSwitch = idSwitch;
    this.estado = estado;
    this.aplicadoEn = aplicadoEn;
  }

  /** Devuelve el identificador del switch. */
  public String getIdSwitch() {
    return idSwitch;
  }

  /** Devuelve el estado actual del switch. */
  public EstadoSwitch getEstado() {
    return estado;
  }

  /** Devuelve la fecha en que se aplicó el estado. */
  public Instant getAplicadoEn() {
    return aplicadoEn;
  }
}
