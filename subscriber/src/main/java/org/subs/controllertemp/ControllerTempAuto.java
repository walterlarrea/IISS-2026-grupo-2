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
        if (temperaturaActual > temperaturaEsperada) {
            switchClient.apagar(idSwitch);
        } else if (temperaturaActual < temperaturaEsperada) {
            switchClient.encender(idSwitch);
        }
    }

    public void controlarHab(double temperaturaActual, String idTermo) {
        if (!activo) {
            return;
        }
        Room room = roomClient.obtenerHabPorTermo(idTermo);
        controlarTemp(temperaturaActual, room.getTemperaturaEsperada(), room.getIdSwitch());
    }
}
