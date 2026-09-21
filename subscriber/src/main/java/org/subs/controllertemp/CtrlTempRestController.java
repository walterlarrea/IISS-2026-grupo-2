package org.subs.controllertemp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/controller")
public class CtrlTempRestController {
    private final ControllerTempAuto controller;

    public CtrlTempRestController(ControllerTempAuto controller) {
        this.controller = controller;
    }

    @PostMapping("/start")
    public ResponseEntity<?> iniciar() {
        controller.iniciar();
        return ResponseEntity.ok(Map.of("estado", "activo"));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> detener() {
        controller.detener();
        return ResponseEntity.ok(Map.of("estado", "detenido"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> estado() {
        return ResponseEntity.ok(Map.of("activo", controller.isActivo()));
    }
}
