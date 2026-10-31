package com.iiss.sensores.grupo_2.switches;

import java.time.Instant;

public class Switch {

  private final String id;
  private EstadoSwitch estado;
  private Instant aplicadoEn;

  public Switch(String id, EstadoSwitch estado, Instant aplicadoEn) {
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

  public Instant getAplicadoEn() {
    return aplicadoEn;
  }

  public void setAplicadoEn(Instant aplicadoEn) {
    this.aplicadoEn = aplicadoEn;
  }
}
