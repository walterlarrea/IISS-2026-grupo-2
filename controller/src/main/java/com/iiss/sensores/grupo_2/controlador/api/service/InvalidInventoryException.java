package com.iiss.sensores.grupo_2.controlador.api.service;

import java.util.List;

public class InvalidInventoryException extends RuntimeException {
  private final List<String> errores;

  public InvalidInventoryException(List<String> errores) {
    super("inventory inválido");
    this.errores = List.copyOf(errores);
  }

  public List<String> getErrores() {
    return errores;
  }
}
