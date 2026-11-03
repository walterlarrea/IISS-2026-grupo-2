package com.iiss.sensores.grupo_2.switches.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

/** Convierte errores de validación y formato en respuestas HTTP comprensibles. */
@RestControllerAdvice
public class RestExceptionHandler {

  /** Responde con HTTP 400 cuando el cuerpo de la solicitud no es válido. */
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "La petición no tiene un formato válido"));
  }
}
