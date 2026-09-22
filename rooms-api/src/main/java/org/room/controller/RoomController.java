package org.room.controller;

import org.room.Room;
import org.room.service.HabitacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/habitaciones")
public class RoomController {

    private final HabitacionService service;

    public RoomController(HabitacionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Room> listar() {
        return service.listar();
    }

    @GetMapping("/validar")
    public List<Room> validar() {
        return service.validar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> buscarPorId(@PathVariable("id") String id) {
        return service.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/termostato/{idTermostato}")
    public ResponseEntity<Room> buscarPorTermostato(@PathVariable("idTermostato") String idTermostato) {
        return service.buscarPorIdTermostato(idTermostato)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Room crear(@RequestBody Room room) {
        return service.crear(room);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> modificar(@PathVariable("id") String id, @RequestBody Room room) {
        return service.modificar(id, room)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        if (!service.eliminar(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Room> modificarParcialmente(@PathVariable("id") String id, @RequestBody Map<String, Object> cambios) {
        return service.modificarParcialmente(id, cambios)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}