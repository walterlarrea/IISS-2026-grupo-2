package com.iiss.sensores.grupo_2.controlador.api.service;

import java.util.List;

/** Error que agrupa las razones por las que un inventario no es válido. */
public class InvalidInventoryException extends RuntimeException {
  private final List<String> errores;

  /** Crea el error y conserva una copia inmutable de sus detalles. */
  public InvalidInventoryException(List<String> errores) {
    super("inventory inválido");
    this.errores = List.copyOf(errores);
  }

  /** Devuelve los detalles de validación del inventario. */
  public List<String> getErrores() {
    return errores;
  }
}
