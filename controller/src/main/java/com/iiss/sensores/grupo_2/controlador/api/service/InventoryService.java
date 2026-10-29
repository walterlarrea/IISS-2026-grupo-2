package com.iiss.sensores.grupo_2.controlador.api.service;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.model.InventoryDocument;
import com.iiss.sensores.grupo_2.controlador.api.repository.InventoryRepository;
import com.iiss.sensores.grupo_2.controlador.api.event.UpdatedInventoryEvent;
import com.iiss.sensores.grupo_2.controlador.engine.service.InventoryMapper;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class InventoryService {
  private static final String INVENTARIO_ACTIVO_ID = "activo";

  private final InventoryRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final SiteStateService siteStateService;
  private final InventoryMapper inventoryMapper;

  public InventoryService(
      InventoryRepository repository,
      ApplicationEventPublisher eventPublisher,
      SiteStateService siteStateService,
      InventoryMapper inventoryMapper) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.siteStateService = siteStateService;
    this.inventoryMapper = inventoryMapper;
  }

  public Optional<Inventory> obtenerActivo() {
    return repository.findById(INVENTARIO_ACTIVO_ID).map(InventoryDocument::inventory);
  }

  @CacheEvict(value = "habitaciones_termostato", allEntries = true)
  public Inventory reemplazar(Inventory inventory) {
    validar(inventory);

    repository.save(new InventoryDocument(INVENTARIO_ACTIVO_ID, inventory));

    siteStateService.loadSiteState(inventoryMapper.convertir(inventory));

    eventPublisher.publishEvent(new UpdatedInventoryEvent(inventory));

    return inventory;
  }

  private void validar(Inventory inventory) {
    List<String> errores = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    Set<String> termostatos = new HashSet<>();
    Set<String> switches = new HashSet<>();

    for (Inventory.Habitacion habitacion : inventory.habitaciones()) {
      if (!ids.add(habitacion.id())) {
        errores.add("id duplicado: '" + habitacion.id() + "'");
      }
      if (!termostatos.add(habitacion.idTermostato())) {
        errores.add("idTermostato duplicado: '" + habitacion.idTermostato() + "'");
      }
      if (!switches.add(habitacion.idSwitch())) {
        errores.add("idSwitch duplicado: '" + habitacion.idSwitch() + "'");
      }
      if (!esUrlValida(habitacion.urlSwitch())) {
        errores.add(
            "habitación '" + habitacion.id() + "': urlSwitch debe ser una URL HTTP o HTTPS válida");
      }
      if (!Double.isFinite(habitacion.temperaturaEsperada())) {
        errores.add("habitación '" + habitacion.id() + "': temperaturaEsperada debe ser finita");
      }
      if (!Double.isFinite(habitacion.potenciaKW()) || habitacion.potenciaKW() <= 0) {
        errores.add("habitación '" + habitacion.id() + "': potenciaKW debe ser > 0");
      }
    }

    if (!Double.isFinite(inventory.sitio().potenciaContratadaKW())
        || inventory.sitio().potenciaContratadaKW() <= 0) {
      errores.add("site: potenciaContratadaKW debe ser > 0");
    }
    Inventory.Franja punta = inventory.sitio().tarifa().punta();
    if (!esFranjaValida(punta)) {
      errores.add("fee.rushHour.desde debe ser anterior a fee.rushHour.hasta");
    }

    if (!errores.isEmpty()) {
      throw new InvalidInventoryException(errores);
    }
  }

  private boolean esFranjaValida(Inventory.Franja franja) {
    try {
      return LocalTime.parse(franja.desde()).isBefore(LocalTime.parse(franja.hasta()));
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private boolean esUrlValida(String valor) {
    try {
      URI uri = URI.create(valor);
      return uri.isAbsolute()
          && uri.getHost() != null
          && ("http".equalsIgnoreCase(uri.getScheme())
              || "https".equalsIgnoreCase(uri.getScheme()));
    } catch (RuntimeException exception) {
      return false;
    }
  }
}
