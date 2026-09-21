package org.switches.controller;

import jakarta.validation.Valid;
import org.switches.dto.SwitchRequest;
import org.switches.dto.SwitchResponse;
import org.switches.service.SwitchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/switches")
public class SwitchController {

    private final SwitchService service;

    public SwitchController(SwitchService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public SwitchResponse obtener(@PathVariable("id") String id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public SwitchResponse actualizar(@Valid @RequestBody SwitchRequest request) {
        return service.actualizar(request.getId(), request.getEncendido());
    }
}