package org.example.controller;

import org.example.Habitacion;
import org.example.service.HabitacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/habitaciones")
public class HabitacionController {

    private final HabitacionService service;

    public HabitacionController(HabitacionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Habitacion> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Habitacion> buscarPorId(@PathVariable("id") String id) {
        return service.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Habitacion crear(@RequestBody Habitacion habitacion) {
        return service.crear(habitacion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Habitacion> modificar(
        @PathVariable("id") String id,
        @RequestBody Habitacion habitacion
    ) {
        return service.modificar(id, habitacion)
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
}