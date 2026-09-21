package org.subs.controllertemp;

import org.subs.client.RoomClient;
import org.subs.client.SwitchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ControllerTempAuto {
    private static final Logger logger = LoggerFactory.getLogger(ControllerTempAuto.class);
    private boolean activo = false;
    private final SwitchClient switchClient;
    private final RoomClient roomClient;

    public ControllerTempAuto(SwitchClient switchClient, RoomClient roomClient) {
        this.switchClient = switchClient;
        this.roomClient = roomClient;
    }

    public void iniciar() {
        activo = true;
        logger.info("Controlador iniciado");
    }

    public void detener() {
        activo = false;
        logger.info("Controlador detenido");
    }

    public boolean isActivo() {
        return activo;
    }

    public void controlarTemp(double temperaturaActual, double temperaturaEsperada, String idSwitch) {
        if (!activo) {
            return;
        }
        try {
            if (temperaturaActual > temperaturaEsperada) {
                logger.info("Temperatura actual ({}) > esperada ({}). Apagando switch ID = {}", temperaturaActual, temperaturaEsperada, idSwitch);
                switchClient.apagar(idSwitch);
            } else if (temperaturaActual < temperaturaEsperada) {
                logger.info("Temperatura actual ({}) < esperada ({}). Encendiendo switch ID = {}", temperaturaActual, temperaturaEsperada, idSwitch);
                switchClient.encender(idSwitch);
            }
        } catch (Exception e) {
            logger.error("Error al accionar el switch ID = {}: {}", idSwitch, e.getMessage());
        }
    }

    public void controlarHab(double temperaturaActual, String idTermo) {
        if (!activo) {
            return;
        }
        try {
            Habitacion habitacion = roomClient.obtenerHabPorTermo(idTermo);
            if (habitacion != null && habitacion.getIdSwitch() != null) {
                controlarTemp(temperaturaActual, habitacion.getTemperaturaEsperada(), habitacion.getIdSwitch());
            } else {
                logger.warn("No se encontró habitación o idSwitch para el termostato ID = {}", idTermo);
            }
        } catch (Exception e) {
            logger.error("Error al obtener habitación para el termostato ID = {}: {}", idTermo, e.getMessage());
        }
    }
}
