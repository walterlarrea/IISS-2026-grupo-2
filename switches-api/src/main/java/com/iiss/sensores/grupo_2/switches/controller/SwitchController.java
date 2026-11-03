package com.iiss.sensores.grupo_2.switches.controller;

import jakarta.validation.Valid;
import com.iiss.sensores.grupo_2.switches.dto.SwitchRequest;
import com.iiss.sensores.grupo_2.switches.dto.SwitchResponse;
import com.iiss.sensores.grupo_2.switches.service.SwitchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Atiende las solicitudes HTTP de consulta y cambio de estado de switches. */
@RestController
@RequestMapping("/switches")
public class SwitchController {

  private final SwitchService service;

  /** Crea el controlador HTTP con el servicio de switches. */
  public SwitchController(SwitchService service) {
    this.service = service;
  }

  /** Consulta el estado actual del switch identificado. */
  @GetMapping("/{id}")
  public SwitchResponse obtener(@PathVariable("id") String id) {
    return service.obtener(id);
  }

  /** Aplica al switch la acción enviada en la solicitud. */
  @PostMapping("/{id}/comandos")
  @ResponseStatus(HttpStatus.OK)
  public SwitchResponse actualizar(
      @PathVariable("id") String id, @Valid @RequestBody SwitchRequest request) {
    return service.actualizar(id, request.getAccion());
  }
}
