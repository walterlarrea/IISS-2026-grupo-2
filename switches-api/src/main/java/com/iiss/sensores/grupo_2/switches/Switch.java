package com.iiss.sensores.grupo_2.switches;

import java.time.Instant;

/** Mantiene el estado y la última fecha de aplicación de un switch. */
public class Switch {

  private final String id;
  private EstadoSwitch estado;
  private Instant aplicadoEn;

  /** Crea el estado de un switch con sus valores iniciales. */
  public Switch(String id, EstadoSwitch estado, Instant aplicadoEn) {
    this.id = id;
    this.estado = estado;
    this.aplicadoEn = aplicadoEn;
  }

  /** Devuelve el identificador del switch. */
  public String getId() {
    return id;
  }

  /** Devuelve el estado actual del switch. */
  public EstadoSwitch getEstado() {
    return estado;
  }

  /** Actualiza el estado actual del switch. */
  public void setEstado(EstadoSwitch estado) {
    this.estado = estado;
  }

  /** Devuelve cuándo se aplicó por última vez el estado. */
  public Instant getAplicadoEn() {
    return aplicadoEn;
  }

  /** Actualiza la fecha de aplicación del estado. */
  public void setAplicadoEn(Instant aplicadoEn) {
    this.aplicadoEn = aplicadoEn;
  }
}
