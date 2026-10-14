package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import com.iiss.sensores.grupo_2.controlador.api.service.SitioNoConfiguradoException;
import com.iiss.sensores.grupo_2.controlador.engine.service.EstadoSitioService;
import com.iiss.sensores.grupo_2.controlador.engine.service.InventarioMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SiteController {
    private final InventarioService inventarioService;
    private final InventarioMapper inventarioMapper;
    private final EstadoSitioService estadoSitioService;

    public SiteController(InventarioService inventarioService, InventarioMapper inventarioMapper, EstadoSitioService estadoSitioService) {
        this.inventarioService = inventarioService;
        this.inventarioMapper = inventarioMapper;
        this.estadoSitioService = estadoSitioService;
    }

    @GetMapping("/sitio")
    public ResponseEntity<Inventario> obtenerSitio() {
        return inventarioService.obtenerActivo()
                .map(ResponseEntity::ok)
                .orElseThrow(SitioNoConfiguradoException::new);
    }

    @PutMapping("/sitio")
    public ResponseEntity<Map<String, Object>> reemplazarSitio(@Valid @RequestBody Inventario inventario) {
        //validar que el inventario pueda representarse en el core antes de persistirlo.
        inventarioMapper.convertir(inventario);
        Inventario aplicado = inventarioService.reemplazar(inventario);
        estadoSitioService.cargarEstado(inventarioMapper.convertir(aplicado));
        return ResponseEntity.ok(Map.of("sitio", aplicado.sitio().id(), "habitaciones", aplicado.habitaciones().size()));
    }
}
