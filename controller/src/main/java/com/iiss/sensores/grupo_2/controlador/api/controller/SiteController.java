package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.api.service.SiteNotConfigException;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import com.iiss.sensores.grupo_2.controlador.engine.service.InventoryMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SiteController {
    private final InventoryService inventoryService;
    private final InventoryMapper inventoryMapper;
    private final SiteStateService siteStateService;

    public SiteController(InventoryService inventoryService, InventoryMapper inventoryMapper, SiteStateService siteStateService) {
        this.inventoryService = inventoryService;
        this.inventoryMapper = inventoryMapper;
        this.siteStateService = siteStateService;
    }

    @GetMapping("/site")
    public ResponseEntity<Inventory> obtenerSitio() {
        return inventoryService.obtenerActivo()
                .map(ResponseEntity::ok)
                .orElseThrow(SiteNotConfigException::new);
    }

    @PutMapping("/site")
    public ResponseEntity<Map<String, Object>> reemplazarSitio(@Valid @RequestBody Inventory inventory) {
        //validar que el inventory pueda representarse en el core antes de persistirlo.
        inventoryMapper.convertir(inventory);
        Inventory aplicado = inventoryService.reemplazar(inventory);
        siteStateService.loadSiteState(inventoryMapper.convertir(aplicado));
        return ResponseEntity.ok(Map.of("site", aplicado.sitio().id(), "rooms", aplicado.habitaciones().size()));
    }
}
