package com.iiss.sensores.grupo_2.controlador.api.service;

/** Error que indica que todavía no existe un inventario activo del sitio. */
public class SiteNotConfigException extends RuntimeException {
  /** Crea el error con el mensaje estándar de sitio no configurado. */
  public SiteNotConfigException() {
    super("Todavía no se cargó ningún site");
  }
}
