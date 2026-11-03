package com.iiss.sensores.grupo_2.controlador.api.service;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.shared.RoomProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Consulta y modifica las habitaciones dentro del inventario activo. */
@Service
public class RoomService implements RoomProvider {

  private final InventoryService inventoryService;

  /** Crea el servicio con acceso al inventario persistido. */
  public RoomService(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  /** Devuelve la lista de habitaciones del inventario activo. */
  public List<Inventory.Habitacion> listar() {
    Optional<Inventory> inventarioActivo = inventoryService.obtenerActivo();
    return inventarioActivo.map(Inventory::habitaciones).orElse(null);
  }

  /** Busca una habitación por su identificador. */
  public Optional<Inventory.Habitacion> buscarPorId(String id) {
    Optional<Inventory> inventarioActivo = inventoryService.obtenerActivo();
    return inventarioActivo.flatMap(
        inventario ->
            inventario.habitaciones().stream()
                .filter(habitacion -> habitacion.id().equals(id))
                .findFirst());
  }

  /** Agrega una habitación si su identificador aún no existe. */
  @CacheEvict(value = "habitaciones_termostato", allEntries = true)
  public Inventory.Habitacion crear(Inventory.Habitacion habitacion) {
    Inventory inventory = inventoryService.obtenerActivo().orElseThrow(SiteNotConfigException::new);

    Optional<Inventory.Habitacion> existente =
        inventory.habitaciones().stream()
            .filter(actual -> actual.id().equals(habitacion.id()))
            .findFirst();
    if (existente.isPresent()) {
      return existente.get();
    }

    List<Inventory.Habitacion> habitaciones = new ArrayList<>(inventory.habitaciones());
    habitaciones.add(habitacion);
    inventoryService.reemplazar(
        new Inventory(inventory.schemaVersion(), inventory.sitio(), habitaciones));
    return habitacion;
  }

  /** Reemplaza la configuración de una habitación existente. */
  @CacheEvict(value = "habitaciones_termostato", allEntries = true)
  public Optional<Inventory.Habitacion> modificar(String id, Inventory.Habitacion habitacion) {
    Optional<Inventory> inventarioActivo = inventoryService.obtenerActivo();
    if (inventarioActivo.isEmpty()) {
      return Optional.empty();
    }

    Inventory inventory = inventarioActivo.get();
    List<Inventory.Habitacion> habitaciones = new ArrayList<>(inventory.habitaciones());
    for (int i = 0; i < habitaciones.size(); i++) {
      if (habitaciones.get(i).id().equals(id)) {
        Inventory.Habitacion actualizada =
            new Inventory.Habitacion(
                id,
                habitacion.nombre(),
                habitacion.temperaturaEsperada(),
                habitacion.potenciaKW(),
                habitacion.idTermostato(),
                habitacion.topicTermostato(),
                habitacion.idSwitch(),
                habitacion.urlSwitch());
        habitaciones.set(i, actualizada);
        inventoryService.reemplazar(
            new Inventory(inventory.schemaVersion(), inventory.sitio(), habitaciones));
        return Optional.of(actualizada);
      }
    }
    return Optional.empty();
  }

  /** Elimina una habitación por su identificador. */
  @CacheEvict(value = "habitaciones_termostato", allEntries = true)
  public boolean eliminar(String id) {
    Optional<Inventory> inventarioActivo = inventoryService.obtenerActivo();
    if (inventarioActivo.isEmpty()) {
      return false;
    }

    Inventory inventory = inventarioActivo.get();
    List<Inventory.Habitacion> habitaciones = new ArrayList<>(inventory.habitaciones());
    if (!habitaciones.removeIf(habitacion -> habitacion.id().equals(id))) {
      return false;
    }
    inventoryService.reemplazar(
        new Inventory(inventory.schemaVersion(), inventory.sitio(), habitaciones));
    return true;
  }

  /** Busca la habitación asociada a un termostato usando la caché. */
  @Override
  @Cacheable(value = "habitaciones_termostato", key = "#idTermostato")
  public Optional<Inventory.Habitacion> obtenerPorTermostato(String idTermostato) {
    if (inventoryService != null) {
      Optional<Inventory> inventarioActivo = inventoryService.obtenerActivo();
      if (inventarioActivo.isPresent()) {
        return inventarioActivo.get().habitaciones().stream()
            .filter(habitacion -> habitacion.idTermostato().equals(idTermostato))
            .findFirst();
      }
    }
    return Optional.empty();
  }

  /** Aplica solo los campos recibidos y persiste la habitación actualizada. */
  @CacheEvict(value = "habitaciones_termostato", allEntries = true)
  public Optional<Inventory.Habitacion> modificarParcialmente(
      String id, Map<String, Object> cambios) {

    Optional<Inventory> inventarioActivo = inventoryService.obtenerActivo();
    if (inventarioActivo.isEmpty()) {
      return Optional.empty();
    }

    Inventory inventory = inventarioActivo.get();
    List<Inventory.Habitacion> habitaciones = new ArrayList<>(inventory.habitaciones());
    for (int i = 0; i < habitaciones.size(); i++) {
      Inventory.Habitacion existente = habitaciones.get(i);
      if (!existente.id().equals(id)) {
        continue;
      }

      String urlSwitchKey = cambios.containsKey("urlSwitch") ? "urlSwitch" : "uriSwitch";
      Inventory.Habitacion actualizada =
          new Inventory.Habitacion(
              id,
              cambios.containsKey("nombre") ? (String) cambios.get("nombre") : existente.nombre(),
              cambios.containsKey("temperaturaEsperada")
                  ? ((Number) cambios.get("temperaturaEsperada")).doubleValue()
                  : existente.temperaturaEsperada(),
              cambios.containsKey("potenciaKW")
                  ? ((Number) cambios.get("potenciaKW")).doubleValue()
                  : existente.potenciaKW(),
              cambios.containsKey("idTermostato")
                  ? (String) cambios.get("idTermostato")
                  : existente.idTermostato(),
              cambios.containsKey("topicTermostato")
                  ? (String) cambios.get("topicTermostato")
                  : existente.topicTermostato(),
              cambios.containsKey("idSwitch")
                  ? (String) cambios.get("idSwitch")
                  : existente.idSwitch(),
              cambios.containsKey(urlSwitchKey)
                  ? (String) cambios.get(urlSwitchKey)
                  : existente.urlSwitch());
      habitaciones.set(i, actualizada);
      inventoryService.reemplazar(
          new Inventory(inventory.schemaVersion(), inventory.sitio(), habitaciones));
      return Optional.of(actualizada);
    }
    return Optional.empty();
  }

  /** Devuelve las habitaciones inválidas o con termostatos/URLs duplicados. */
  public List<Inventory.Habitacion> validar() {
    List<Inventory.Habitacion> habitaciones =
        inventoryService.obtenerActivo().map(Inventory::habitaciones).orElseGet(List::of);
    Set<Inventory.Habitacion> noCumplen = new LinkedHashSet<>();

    Map<String, List<Inventory.Habitacion>> porTermostato = new LinkedHashMap<>();
    Map<String, List<Inventory.Habitacion>> porUrlSwitch = new LinkedHashMap<>();

    for (Inventory.Habitacion habitacion : habitaciones) {
      boolean invalido = false;

      if (habitacion.idTermostato() == null || habitacion.idTermostato().isBlank()) {
        invalido = true;
      } else {
        porTermostato
            .computeIfAbsent(habitacion.idTermostato(), k -> new ArrayList<>())
            .add(habitacion);
      }

      if (!esUriValida(habitacion.urlSwitch())) {
        invalido = true;
      } else {
        porUrlSwitch
            .computeIfAbsent(habitacion.urlSwitch(), k -> new ArrayList<>())
            .add(habitacion);
      }

      if (invalido) {
        noCumplen.add(habitacion);
      }
    }

    for (List<Inventory.Habitacion> grupo : porTermostato.values()) {
      if (grupo.size() > 1) {
        noCumplen.addAll(grupo);
      }
    }

    for (List<Inventory.Habitacion> grupo : porUrlSwitch.values()) {
      if (grupo.size() > 1) {
        noCumplen.addAll(grupo);
      }
    }

    return new ArrayList<>(noCumplen);
  }

  /** Comprueba que la URL sea absoluta y use HTTP o HTTPS con un host. */
  private boolean esUriValida(String uriStr) {
    if (uriStr == null || uriStr.isBlank()) {
      return false;
    }
    try {
      URI uri = new URI(uriStr);
      if (!uri.isAbsolute()) {
        return false;
      }
      String scheme = uri.getScheme();
      if (scheme == null
          || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        return false;
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        return false;
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
