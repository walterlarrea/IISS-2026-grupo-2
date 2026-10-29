package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.engine.utils.CustomClock;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import com.iiss.sensores.grupo_2.modelos.input.Room;
import com.iiss.sensores.grupo_2.modelos.output.Command;
import com.iiss.sensores.grupo_2.modelos.output.CommandSwitch;
import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.api.model.StateControl;
import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZonedDateTime;

/** Handles time & simulated time, uses the Core to calculate action commands and executes them */
@Service
public class TemperatureControlEngine implements TemperatureEngineService {
  private static final Logger logger = LoggerFactory.getLogger(TemperatureControlEngine.class);
  //  private static final ZoneId ZONA_MONTEVIDEO = ZoneId.of("America/Montevideo");
  private final SwitchClient switchClient;
  private final ICore core;
  private final SiteStateService siteStateService;
  private volatile boolean active = false;
  private CustomClock clock;

  public TemperatureControlEngine(
      SwitchClient switchClient, ICore core, SiteStateService siteStateService) {
    this.switchClient = switchClient;
    this.core = core;
    this.siteStateService = siteStateService;
  }

  @Override
  public void start(Instant setDateTime, int timeFactor) {
    if (setDateTime == null) {
      throw new IllegalArgumentException("La fecha y hora de inicio es obligatoria");
    }

    if (timeFactor <= 0) {
      throw new IllegalArgumentException("Time factor must be a positive number");
    }

    this.active = true;

    this.clock = new CustomClock(setDateTime, timeFactor);

    logger.info("Temperature Control Engine was INITIATED");
    logger.info("Simulated date: {}, time factor: {}", setDateTime, timeFactor);
  }

  @Override
  public void stop() {
    active = false;
    this.clock = null;

    logger.info("Temperature Control Engine was STOPPED");
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public StateControl getTempControllerStatus() {
    if (!active) {
      return new StateControl(StateControl.Estado.DETENIDO, null, null, null);
    }

    return new StateControl(
        StateControl.Estado.EN_EJECUCION,
        this.clock.getTime(),
        this.clock.getTimeFactor(),
        this.clock.getStartedAt());
  }

  /** Request decisions from Core and executes it's commands */
  public synchronized void execute() {
    if (!active) {
      logger.debug("Controller is stopped");
      return;
    }

    try {
      DataSite siteState = siteStateService.getSiteState();

      ZonedDateTime simulatedDateTime = this.clock.getTimeZoned();

      CoreAnswer coreResponse = core.calcularComandos(simulatedDateTime, siteState);

      if (coreResponse == null || coreResponse.commands() == null) {
        logger.warn("Core response is null");
        return;
      }

      for (Command command : coreResponse.commands()) {
        if (!(command instanceof CommandSwitch commandSwitch)) {
          logger.debug("Command in Core response is of an unknown type");
          continue;
        }

        Room room =
            siteState.habitaciones().stream()
                .filter(h -> h.idSwitch().equals(commandSwitch.idSwitch()))
                .findFirst()
                .orElse(null);

        if (room == null) {
          logger.warn("Room not found for Switch with ID = {}", commandSwitch.idSwitch());
          continue;
        }

        try {
          if (commandSwitch.estado() == SwitchState.ENCENDIDO) {
            switchClient.encender(room);
            logger.info("Turned ON the switch with ID = {}", commandSwitch.idSwitch());
          } else if (commandSwitch.estado() == SwitchState.APAGADO) {
            switchClient.apagar(room);
            logger.info("Turned OFF the switch with ID = {}", commandSwitch.idSwitch());
          } else {
            logger.warn("Switch state not recognized: {}", commandSwitch.estado());
            continue;
          }

          // Switch status is updated only when the operation was successful
          siteStateService.updateSwitchState(commandSwitch.idSwitch(), commandSwitch.estado());
          logger.info(
              "Switch {} state updated to {}", commandSwitch.idSwitch(), commandSwitch.estado());

        } catch (Exception e) {
          logger.error(
              "Error executing a command to the switch {}, with url {}",
              commandSwitch.idSwitch(),
              room.urlSwitch());
        }
      }
    } catch (Exception e) {
      logger.error("Error in the automatic temperature control cycle", e);
    }
  }

  /**
   * Processes a temperature reading message. Updates the site state and executes a control cycle.
   *
   * @param temperatureMsg Parsed temperature reading from MQTT
   */
  public void processTemperature(TemperatureMessage temperatureMsg) {
    if (!active) {
      logger.debug(
          "Controller is stopped, ignores temperature sent by thermostat with ID {}",
          temperatureMsg.idThermostat());
      return;
    }

    if (temperatureMsg.idThermostat() == null
        || temperatureMsg.idThermostat().isBlank()
        || !Double.isFinite(temperatureMsg.tC())) {
      logger.warn(
          "Invalid reading data. Thermostat ID: {}, temperature Cº: {}",
          temperatureMsg.idThermostat(),
          temperatureMsg.tC());
      return;
    }

    try {
      siteStateService.updateRoomTemperature(temperatureMsg.idThermostat(), temperatureMsg.tC());
      execute();

    } catch (Exception e) {
      logger.error(
          "Error processing temperature change for thermostat ID {}",
          temperatureMsg.idThermostat(),
          e);
    }
  }
}
