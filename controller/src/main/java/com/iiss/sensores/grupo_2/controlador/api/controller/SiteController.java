package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.api.service.SiteNotConfigException;
import com.iiss.sensores.grupo_2.controlador.engine.service.InventoryMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SiteController {
  private final InventoryService inventoryService;
  private final InventoryMapper inventoryMapper;

  public SiteController(InventoryService inventoryService, InventoryMapper inventoryMapper) {
    this.inventoryService = inventoryService;
    this.inventoryMapper = inventoryMapper;
  }

  @GetMapping("/sitio")
  public ResponseEntity<Inventory> obtenerSitio() {
    return inventoryService
        .obtenerActivo()
        .map(ResponseEntity::ok)
        .orElseThrow(SiteNotConfigException::new);
  }

  @PutMapping("/sitio")
  public ResponseEntity<Map<String, Object>> reemplazarSitio(
      @Valid @RequestBody Inventory inventory) {
    // Validate the payload shape
    inventoryMapper.convertir(inventory);
    // TODO: validation error -> response handling?

    Inventory savedInventory = inventoryService.reemplazar(inventory);

    return ResponseEntity.ok(
        Map.of("site", savedInventory.sitio().id(), "rooms", savedInventory.habitaciones().size()));
  }
}
