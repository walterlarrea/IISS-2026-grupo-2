package com.iiss.sensores.grupo_2.controlador.api.service;

import java.util.List;

/** Error que contiene el mensaje y los detalles de una solicitud inválida. */
public class InvalidRequestException extends RuntimeException {
  private final List<String> errores;

  /** Crea el error con su mensaje y una copia de los detalles. */
  public InvalidRequestException(String mensaje, List<String> errores) {
    super(mensaje);
    this.errores = List.copyOf(errores);
  }

  /** Devuelve los detalles asociados a la solicitud inválida. */
  public List<String> getErrores() {
    return errores;
  }
}
