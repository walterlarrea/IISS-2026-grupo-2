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

/** Rest commands for engine */
@RestController
public class EngineController {
  private final TemperatureEngineService engineService;

  public EngineController(TemperatureEngineService engineService) {
    this.engineService = engineService;
  }

  @GetMapping("/control")
  public StateControl obtenerControl() {
    return engineService.getTempControllerStatus();
  }

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

  private InvalidRequestException comandoInvalido(String error) {
    return new InvalidRequestException("comando inválido", List.of(error));
  }
}
