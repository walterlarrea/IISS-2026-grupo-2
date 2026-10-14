package com.iiss.sensores.grupo_2.controlador.api.controller;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
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

@RestController
@RequestMapping("/habitaciones")
public class RoomController {

    private final RoomService roomsService;

    public RoomController(RoomService roomsService) {
        this.roomsService = roomsService;
    }

    @GetMapping
    public List<Inventario.Habitacion> listar() {
        return roomsService.listar();
    }

    @GetMapping("/validar")
    public List<Inventario.Habitacion> validar() {
        return roomsService.validar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventario.Habitacion> buscarPorId(@PathVariable("id") String id) {
        return roomsService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/termostato/{idTermostato}")
    public ResponseEntity<Inventario.Habitacion> buscarPorTermostato(@PathVariable("idTermostato") String idTermostato) {
        return roomsService.obtenerPorTermostato(idTermostato)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Inventario.Habitacion crear(@RequestBody Inventario.Habitacion habitacion) {
        return roomsService.crear(habitacion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inventario.Habitacion> modificar(
            @PathVariable("id") String id,
            @RequestBody Inventario.Habitacion habitacion) {
        return roomsService.modificar(id, habitacion)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        if (!roomsService.eliminar(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Inventario.Habitacion> modificarParcialmente(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> cambios) {
        return roomsService.modificarParcialmente(id, cambios)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
