package com.iiss.sensores.grupo_2.controlador.api.service;

public class SiteNotConfigException extends RuntimeException {
  public SiteNotConfigException() {
    super("Todavía no se cargó ningún site");
  }
}
