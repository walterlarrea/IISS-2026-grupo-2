package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.CommandControl;
import com.iiss.sensores.grupo_2.controlador.api.model.StateControl;
import com.iiss.sensores.grupo_2.controlador.api.service.InvalidRequestException;
import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/** Expone endpoints REST para consultar y comandar el motor de temperatura. */
@RestController
public class EngineController {
  private final TemperatureEngineService engineService;

  /** Crea el controlador HTTP con el servicio del motor. */
  public EngineController(TemperatureEngineService engineService) {
    this.engineService = engineService;
  }

  /** Devuelve el estado actual del motor. */
  @GetMapping("/control")
  public StateControl obtenerControl() {
    return engineService.getTempControllerStatus();
  }

  /** Valida y ejecuta una orden para iniciar o detener el motor. */
  @PostMapping("/control/comandos")
  public StateControl comandarControl(@RequestBody CommandControl comando) {
    if (comando.accion() == null) {
      throw comandoInvalido("accion es obligatoria");
    }
    switch (comando.accion()) {
      case "INICIAR" -> start(comando);
      case "DETENER" -> engineService.stop();
      default -> throw comandoInvalido("accion desconocida: " + comando.accion());
    }
    return engineService.getTempControllerStatus();
  }

  /** Valida los parámetros e inicia el motor con la hora y factor indicados. */
  private void start(CommandControl comando) {
    if (comando.fechaHora() == null) {
      throw comandoInvalido("INICIAR requiere fechaHora");
    }
    int timeFactor = Optional.ofNullable(comando.factorTiempo()).orElse(1);
    if (timeFactor <= 0) {
      throw comandoInvalido("factorTiempo must be a positive number");
    }
    engineService.start(comando.fechaHora(), timeFactor);
  }

  /** Construye el error estándar para una orden inválida. */
  private InvalidRequestException comandoInvalido(String error) {
    return new InvalidRequestException("comando inválido", List.of(error));
  }
}
