package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.api.model.EstadoControl;
import com.iiss.sensores.grupo_2.controlador.shared.RoomInfo;
import com.iiss.sensores.grupo_2.controlador.shared.RoomProvider;
import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class ControllerTempAuto implements TemperatureEngineService {
    private static final Logger logger = LoggerFactory.getLogger(ControllerTempAuto.class);
    private volatile boolean activo = false;
    private Instant fechaHoraBase;
    private Instant iniciadoEn;
    private double factorTiempo;
    private final SwitchClient switchClient;
    private final RoomProvider roomProvider;

    public ControllerTempAuto(SwitchClient switchClient, RoomProvider roomProvider) {
        this.switchClient = switchClient;
        this.roomProvider = roomProvider;
    }

    @Override
    public synchronized void iniciar() {
        iniciar(Instant.now(), 1.0);
    }

    @Override
    public synchronized void iniciar(Instant fechaHora, double factorTiempo) {
        this.fechaHoraBase = fechaHora;
        this.iniciadoEn = Instant.now();
        this.factorTiempo = factorTiempo;
        activo = true;
        logger.info("Controlador iniciado");
    }

    @Override
    public synchronized void detener() {
        activo = false;
        fechaHoraBase = null;
        iniciadoEn = null;
        factorTiempo = 0;
        logger.info("Controlador detenido");
    }

    @Override
    public boolean isActivo() {
        return activo;
    }

    @Override
    public synchronized EstadoControl obtenerEstado() {
        if (!activo) {
            return new EstadoControl(EstadoControl.Estado.DETENIDO, null, null, null);
        }
        long tiempoRealNanos = Duration.between(iniciadoEn, Instant.now()).toNanos();
        Instant fechaHoraActual = fechaHoraBase.plusNanos((long) (tiempoRealNanos * factorTiempo));
        return new EstadoControl(
                EstadoControl.Estado.EN_EJECUCION,
                fechaHoraActual,
                factorTiempo,
                iniciadoEn
        );
    }

    public void controlarTemp(double temperaturaActual, double temperaturaEsperada, String uriSwitch) {
        if (!activo) {
            return;
        }
        try {
            if (temperaturaActual > temperaturaEsperada) {
                logger.info("Temperatura actual ({}) > esperada ({}). Apagando switch ID = {}", temperaturaActual, temperaturaEsperada, uriSwitch);
                switchClient.apagar(uriSwitch);
            } else if (temperaturaActual < temperaturaEsperada) {
                logger.info("Temperatura actual ({}) < esperada ({}). Encendiendo switch ID = {}", temperaturaActual, temperaturaEsperada, uriSwitch);
                switchClient.encender(uriSwitch);
            }
        } catch (Exception e) {
            logger.error("Error al accionar el switch ID = {}: {}", uriSwitch, e.getMessage());
        }
    }

    public void controlarHab(double temperaturaActual, String idTermo) {
    logger.info(
            "Procesando temperatura: {} °C para el termostato {}",
            temperaturaActual,
            idTermo
    );

    if (!activo) {
        logger.info("Control detenido. Se ignora la temperatura recibida.");
        return;
    }

    Optional<RoomInfo> roomOpt =
            roomProvider.obtenerPorTermostato(idTermo);

    if (roomOpt.isPresent()) {
        RoomInfo room = roomOpt.get();

        logger.info(
                "Habitación encontrada: {}. Temperatura esperada: {} °C. URI del switch: {}",
                room.nombre(),
                room.temperaturaEsperada(),
                room.uriSwitch()
        );

        controlarTemp(
                temperaturaActual,
                room.temperaturaEsperada(),
                room.uriSwitch()
        );
    } else {
        logger.warn(
                "No se encontró habitación configurada para el termostato: {}",
                idTermo
            );
        }
    }
}
