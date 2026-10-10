package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/controller")
public class EngineController {

    private final TemperatureEngineService engineService;

    public EngineController(TemperatureEngineService engineService) {
        this.engineService = engineService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> iniciar() {
        engineService.iniciar();
        return ResponseEntity.ok(Map.of("estado", "activo"));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> detener() {
        engineService.detener();
        return ResponseEntity.ok(Map.of("estado", "detenido"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> estado() {
        return ResponseEntity.ok(Map.of("activo", engineService.isActivo()));
    }
}
