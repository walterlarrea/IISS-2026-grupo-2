package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Carga al iniciar la aplicación el inventario persistido en el estado del motor. */
@Component
public class SiteStateInitializer {
  private static final Logger logger = LoggerFactory.getLogger(SiteStateInitializer.class);
  private final InventoryService inventoryService;
  private final InventoryMapper inventoryMapper;
  private final SiteStateService siteStateService;

  /** Crea el inicializador con los servicios de inventario y estado. */
  public SiteStateInitializer(
      InventoryService inventoryService,
      InventoryMapper inventoryMapper,
      SiteStateService siteStateService) {
    this.inventoryService = inventoryService;
    this.inventoryMapper = inventoryMapper;
    this.siteStateService = siteStateService;
  }

  /** Inicializa el estado en memoria si existe un inventario activo. */
  @EventListener(ApplicationReadyEvent.class)
  public void inicializarEstado() {
    inventoryService
        .obtenerActivo()
        .ifPresentOrElse(
            inventario -> {
              siteStateService.loadSiteState(inventoryMapper.convertir(inventario));
              logger.info("Estado del site inicializado");
            },
            () ->
                logger.warn(
                    "No hay un inventory activo. El controlador no podrá operar hasta configurar el site."));
  }
}
