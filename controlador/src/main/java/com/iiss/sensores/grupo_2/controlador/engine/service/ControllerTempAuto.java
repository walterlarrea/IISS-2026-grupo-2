package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.output.Comando;
import com.iiss.sensores.grupo_2.modelos.output.ComandoSwitch;
import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.api.model.EstadoControl;
import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class ControllerTempAuto implements TemperatureEngineService {
    private static final Logger logger = LoggerFactory.getLogger(ControllerTempAuto.class);
    private final SwitchClient switchClient;
    private final ICore core;
    private final SiteStateService siteStateService; //mantiene los datos actuales del sitio
    private volatile boolean active = false;
    private Instant baseDateTime;
    private Instant startedAt;
    private int timeFactor;

    public ControllerTempAuto(SwitchClient switchClient, ICore core, SiteStateService siteStateService) {
        //conecta el estado con el core, ejecuta los comandos calculados y controla el ciclo de ejecución.
        this.switchClient = switchClient;
        this.core = core;
        this.siteStateService = siteStateService;
    }

    @Override
    public synchronized void start(Instant fechaHora, int timeFactor) {
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora de inicio es obligatoria");
        }

        if (timeFactor <= 0) {
            throw new IllegalArgumentException("Time factor must be a positive number");
        }

        this.baseDateTime = fechaHora;
        this.startedAt = Instant.now();
        this.timeFactor = timeFactor;
        this.active = true;

        logger.info("Controlador iniciado. Fecha simulada: {}, factor: {}", fechaHora, timeFactor);
    }

    @Override
    public synchronized void stop() {
        active = false;
        baseDateTime = null;
        startedAt = null;
        timeFactor = 1;

        logger.info("Controlador detenido");
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public synchronized EstadoControl getTempControllerStatus() {
        if (!active) {
            return new EstadoControl(EstadoControl.Estado.DETENIDO, null, null, null);
        }
        // TODO: Review this logic, make sure it works and it is easily understandable
        long tiempoRealNanos = Duration.between(startedAt, Instant.now()).toNanos();
        Instant fechaHoraActual = baseDateTime.plusNanos((long) (tiempoRealNanos * timeFactor));
        return new EstadoControl(EstadoControl.Estado.EN_EJECUCION, fechaHoraActual, timeFactor, startedAt);
    }

    // solicita las decisiones al core y ejecuta sus comandos, todo el ciclo se serializa para evitar decisiones simultáneas.
    public synchronized void execute() {
        if (!active) {
            logger.debug("Controller is stopped");
            return;
        }

        try {
            // Site state
            DataSitio dataSitio = siteStateService.getSiteState();
            // Temperature Controller Engine status
            EstadoControl estado = getTempControllerStatus();

            // horaSimulada = horaEventoTemperatura * factor
            // delta = horaEventoTemperatura - this.iniciadoEn
            // horaSimulada = horaEventoTemperatura - delta + ( delta * factor )
            // horaSimulada = horaEventoTemperatura + ( delta * factor ) - delta
            // 5:00
            // horaSimulada = 5:10 - 0:10 + ( 0:10 * 2 )

            // hora punta del usuario = 17:00-23:00 -> 14:00-20:00
            ZonedDateTime fechaSimulada = estado.fechaHora().atZone(ZoneId.systemDefault());

            RespuestaCore respuesta = core.calcularComandos(fechaSimulada, dataSitio);

            if (respuesta == null || respuesta.comandos() == null) {
                logger.warn("El core devolvió una respuesta nula");
                return;
            }

            for (Comando comando : respuesta.comandos()) {
                if (!(comando instanceof ComandoSwitch comandoSwitch)) {
                    logger.debug("Se omite un comando que no es de tipo switch");
                    continue;
                }

                Habitacion habitacion = dataSitio.habitaciones().stream()
                        .filter(h -> h.idSwitch().equals(comandoSwitch.idSwitch()))
                        .findFirst()
                        .orElse(null);

                if (habitacion == null) {
                    logger.warn("No se encontró habitación para el switch {}", comandoSwitch.idSwitch());
                    continue;
                }

                try {
                    if (comandoSwitch.estado() == EstadoSwitch.ENCENDIDO) {
                        switchClient.encender(habitacion);
                        logger.info("Controller status: running");
                    } else if (comandoSwitch.estado() == EstadoSwitch.APAGADO) {
                        switchClient.apagar(habitacion);
                        logger.info("Controller status: stopped");
                    } else {
                        logger.warn("Switch state not recognized: {}", comandoSwitch.estado());
                        continue;
                    }

                    //solo se actualiza el estado local si la operación remota terminó sin lanzar una excepción.
                    siteStateService.updateSwitchState(comandoSwitch.idSwitch(), comandoSwitch.estado());
                    logger.info("Switch {} state updated to {}", comandoSwitch.idSwitch(), comandoSwitch.estado());

                } catch (Exception e) {
                    logger.error("Error executing a command to the switch {}, with url {}", comandoSwitch.idSwitch(), habitacion.urlSwitch(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in the automatic temperature control cycle", e);
        }
    }

    //actualiza una temperatura y ejecuta un nuevo ciclo de control.
    public synchronized void processTemperature(TemperatureMessage temperatureMsg) {
        if (!active) {
            logger.debug("Controller stopped, ignores temperature sent by thermostat with ID {}", temperatureMsg.idThermostat());
            return;
        }

        if (temperatureMsg.idThermostat() == null || temperatureMsg.idThermostat().isBlank() || !Double.isFinite(temperatureMsg.tC())) {
            logger.warn("Invalid measurement data. Thermostat ID: {}, temperature Cº: {}", temperatureMsg.idThermostat(), temperatureMsg.tC());
            return;
        }

        try {
            siteStateService.updateRoomTemperature(temperatureMsg.idThermostat(), temperatureMsg.tC());
            execute();

        } catch (Exception e) {
            logger.error("Error processing temperature change for thermostat ID {}", temperatureMsg.idThermostat(), e);
        }
    }

}
