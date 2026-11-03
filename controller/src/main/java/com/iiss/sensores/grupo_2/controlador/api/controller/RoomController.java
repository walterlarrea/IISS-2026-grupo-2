package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Expone endpoints REST para consultar y modificar habitaciones. */
@RestController
@RequestMapping("/habitaciones")
public class RoomController {

  private final RoomService roomsService;

  /** Crea el controlador HTTP con el servicio de habitaciones. */
  public RoomController(RoomService roomsService) {
    this.roomsService = roomsService;
  }

  /** Lista las habitaciones configuradas. */
  @GetMapping
  public List<Inventory.Habitacion> listar() {
    return roomsService.listar();
  }

  /** Devuelve las habitaciones que pasan la validación del servicio. */
  @GetMapping("/validar")
  public List<Inventory.Habitacion> validar() {
    return roomsService.validar();
  }

  /** Busca una habitación por su identificador. */
  @GetMapping("/{id}")
  public ResponseEntity<Inventory.Habitacion> buscarPorId(@PathVariable("id") String id) {
    return roomsService
        .buscarPorId(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** Busca una habitación asociada al identificador de termostato. */
  @GetMapping("/termostato/{idTermostato}")
  public ResponseEntity<Inventory.Habitacion> buscarPorTermostato(
      @PathVariable("idTermostato") String idTermostato) {
    return roomsService
        .obtenerPorTermostato(idTermostato)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** Crea una habitación en el inventario activo. */
  @PostMapping
  public Inventory.Habitacion crear(@RequestBody Inventory.Habitacion habitacion) {
    return roomsService.crear(habitacion);
  }

  /** Reemplaza la configuración de una habitación existente. */
  @PutMapping("/{id}")
  public ResponseEntity<Inventory.Habitacion> modificar(
      @PathVariable("id") String id, @RequestBody Inventory.Habitacion habitacion) {
    return roomsService
        .modificar(id, habitacion)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** Elimina una habitación y devuelve 204 si existía. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
    if (!roomsService.eliminar(id)) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }

  /** Actualiza parcialmente la configuración de una habitación. */
  @PatchMapping("/{id}")
  public ResponseEntity<Inventory.Habitacion> modificarParcialmente(
      @PathVariable("id") String id, @RequestBody Map<String, Object> cambios) {
    return roomsService
        .modificarParcialmente(id, cambios)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
