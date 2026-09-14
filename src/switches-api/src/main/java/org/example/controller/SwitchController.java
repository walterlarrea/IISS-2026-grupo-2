package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.SwitchRequest;
import org.example.dto.SwitchResponse;
import org.example.service.SwitchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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