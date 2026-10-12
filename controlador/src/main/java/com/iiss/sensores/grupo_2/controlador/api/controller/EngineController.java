package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.ComandoControl;
import com.iiss.sensores.grupo_2.controlador.api.model.EstadoControl;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import com.iiss.sensores.grupo_2.controlador.api.service.SitioNoConfiguradoException;
import com.iiss.sensores.grupo_2.controlador.api.service.SolicitudInvalidaException;
import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class EngineController {

    private final TemperatureEngineService engineService;
    private final InventarioService inventarioService;

    public EngineController(TemperatureEngineService engineService, InventarioService inventarioService) {
        this.engineService = engineService;
        this.inventarioService = inventarioService;
    }

    @GetMapping("/sitio")
    public ResponseEntity<Inventario> obtenerSitio() {
        return inventarioService.obtenerActivo()
                .map(ResponseEntity::ok)
                .orElseThrow(SitioNoConfiguradoException::new);
    }

    @PutMapping("/sitio")
    public ResponseEntity<Map<String, Object>> reemplazarSitio(@Valid @RequestBody Inventario inventario) {
        Inventario aplicado = inventarioService.reemplazar(inventario);
        return ResponseEntity.ok(Map.of(
                "sitio", aplicado.sitio().id(),
                "habitaciones", aplicado.habitaciones().size()
        ));
    }

    @GetMapping("/control")
    public EstadoControl obtenerControl() {
        return engineService.obtenerEstado();
    }

    @PostMapping("/control/comandos")
    public EstadoControl comandarControl(@RequestBody ComandoControl comando) {
        if (comando.accion() == null) {
            throw comandoInvalido("accion es obligatoria");
        }
        switch (comando.accion()) {
            case "INICIAR" -> iniciar(comando);
            case "DETENER" -> engineService.detener();
            default -> throw comandoInvalido("accion desconocida: " + comando.accion());
        }
        return engineService.obtenerEstado();
    }

    private void iniciar(ComandoControl comando) {
        if (comando.fechaHora() == null) {
            throw comandoInvalido("INICIAR requiere fechaHora");
        }
        double factorTiempo = Optional.ofNullable(comando.factorTiempo()).orElse(1.0);
        if (!Double.isFinite(factorTiempo) || factorTiempo <= 0) {
            throw comandoInvalido("factorTiempo debe ser un número mayor que cero");
        }
        engineService.iniciar(comando.fechaHora(), factorTiempo);
    }

    private SolicitudInvalidaException comandoInvalido(String error) {
        return new SolicitudInvalidaException("comando inválido", List.of(error));
    }

    @PostMapping("/controller/start")
    public ResponseEntity<?> iniciarLegacy() {
        engineService.iniciar();
        return ResponseEntity.ok(Map.of("estado", "activo"));
    }

    @PostMapping("/controller/stop")
    public ResponseEntity<?> detenerLegacy() {
        engineService.detener();
        return ResponseEntity.ok(Map.of("estado", "detenido"));
    }

    @GetMapping("/controller/status")
    public ResponseEntity<?> estadoLegacy() {
        return ResponseEntity.ok(Map.of("activo", engineService.isActivo()));
    }

}
