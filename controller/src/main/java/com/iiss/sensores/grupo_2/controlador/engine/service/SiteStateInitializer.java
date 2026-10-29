package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SiteStateInitializer {
  // recupera la configuración persistida cuando la aplicación termina de arrancar
  // usa InventoryService y no el simulador
  private static final Logger logger = LoggerFactory.getLogger(SiteStateInitializer.class);
  private final InventoryService inventoryService;
  private final InventoryMapper inventoryMapper;
  private final SiteStateService siteStateService;

  public SiteStateInitializer(
      InventoryService inventoryService,
      InventoryMapper inventoryMapper,
      SiteStateService siteStateService) {
    this.inventoryService = inventoryService;
    this.inventoryMapper = inventoryMapper;
    this.siteStateService = siteStateService;
  }

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
