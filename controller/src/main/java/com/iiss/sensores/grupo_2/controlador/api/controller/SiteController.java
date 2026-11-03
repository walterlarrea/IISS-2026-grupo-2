package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.api.service.SiteNotConfigException;
import com.iiss.sensores.grupo_2.controlador.engine.service.InventoryMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Expone endpoints REST para consultar y reemplazar el inventario del sitio. */
@RestController
public class SiteController {
  private final InventoryService inventoryService;
  private final InventoryMapper inventoryMapper;

  /** Crea el controlador con los servicios de inventario y conversión. */
  public SiteController(InventoryService inventoryService, InventoryMapper inventoryMapper) {
    this.inventoryService = inventoryService;
    this.inventoryMapper = inventoryMapper;
  }

  /** Devuelve el inventario activo o un error si el sitio no está configurado. */
  @GetMapping("/sitio")
  public ResponseEntity<Inventory> obtenerSitio() {
    return inventoryService
        .obtenerActivo()
        .map(ResponseEntity::ok)
        .orElseThrow(SiteNotConfigException::new);
  }

  /** Valida y persiste el nuevo inventario del sitio. */
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
