package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.ComandoControl;
import com.iiss.sensores.grupo_2.controlador.api.model.EstadoControl;
import com.iiss.sensores.grupo_2.controlador.api.service.SolicitudInvalidaException;
import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class EngineController {
    private final TemperatureEngineService engineService;

    public EngineController(TemperatureEngineService engineService) {
        this.engineService = engineService;
    }

    @GetMapping("/control")
    public EstadoControl obtenerControl() {
        return engineService.getTempControllerStatus();
    }

    @PostMapping("/control/comandos")
    public EstadoControl comandarControl(@RequestBody ComandoControl comando) {
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

    private void start(ComandoControl comando) {
        if (comando.fechaHora() == null) {
            throw comandoInvalido("INICIAR requiere fechaHora");
        }
        int timeFactor = Optional.ofNullable(comando.factorTiempo()).orElse(1);
        if (timeFactor <= 0) {
            throw comandoInvalido("factorTiempo must be a positive number");
        }
        engineService.start(comando.fechaHora(), timeFactor);
    }

    private SolicitudInvalidaException comandoInvalido(String error) {
        return new SolicitudInvalidaException("comando inválido", List.of(error));
    }
}
