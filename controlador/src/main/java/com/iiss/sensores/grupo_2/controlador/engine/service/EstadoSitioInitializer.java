package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EstadoSitioInitializer {
    //recupera la configuración persistida cuando la aplicación termina de arrancar
    //usa InventarioService y no el simulador
    private static final Logger logger = LoggerFactory.getLogger(EstadoSitioInitializer.class);
    private final InventarioService inventarioService;
    private final InventarioMapper inventarioMapper;
    private final SiteStateService siteStateService;

    public EstadoSitioInitializer(InventarioService inventarioService, InventarioMapper inventarioMapper, SiteStateService siteStateService) {
        this.inventarioService = inventarioService;
        this.inventarioMapper = inventarioMapper;
        this.siteStateService = siteStateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inicializarEstado() {
        inventarioService.obtenerActivo().ifPresentOrElse(
                inventario -> {
                    siteStateService.loadSiteState(inventarioMapper.convertir(inventario));
                    logger.info("Estado del sitio inicializado");
                },
                () -> logger.warn("No hay un inventario activo. El controlador no podrá operar hasta configurar el sitio.")
        );
    }
}
