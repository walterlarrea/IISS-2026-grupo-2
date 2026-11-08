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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/** Handles time & simulated time, uses the Core to calculate action commands and executes them */
@Service
public class TemperatureControlEngine implements TemperatureEngineService {
  private static final Logger logger = LoggerFactory.getLogger(TemperatureControlEngine.class);
  private final SwitchClient switchClient;
  private final ICore core;
  private final SiteStateService siteStateService;
  private final Clock baseClock;
  private volatile boolean active = false;
  private CustomClock clock;

  /** Period of the scheduled control cycle, see {@link ReevaluationControlService} */
  @Value("${controlador.reevaluacion-ms:5000}")
  private long reevaluacionMs = 5000;

  private static final long CYCLE_MARGIN_MS = 1000;

  @Autowired
  public TemperatureControlEngine(
      SwitchClient switchClient, ICore core, SiteStateService siteStateService) {
    this(switchClient, core, siteStateService, Clock.systemUTC());
  }

  public TemperatureControlEngine(
      SwitchClient switchClient, ICore core, SiteStateService siteStateService, Clock baseClock) {
    this.switchClient = switchClient;
    this.core = core;
    this.siteStateService = siteStateService;
    this.baseClock = Objects.requireNonNull(baseClock, "baseClock cannot be null");
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

    this.clock = new CustomClock(setDateTime, timeFactor, this.baseClock);

    logger.info("Temperature Control Engine was INITIATED");
    logger.info("Simulated date: {}, time factor: {}", setDateTime, timeFactor);
  }

  @Override
  public synchronized void stop() {
    if (active) {
      turnOffAllSwitches();
    }
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

  /**
   * If the rush hour starts before the next cycle, the Core is asked as if it had already started:
   * a switch turned ON now would stay ON into the rush hour until that next cycle. The window is
   * the cycle period (plus a margin for the cycle's own run time) in simulated time.
   */
  private ZonedDateTime timeForCore(ZonedDateTime now, DataSite siteState) {
    if (siteState.site() == null
        || siteState.site().fee() == null
        || siteState.site().fee().isRushHour(now)) {
      return now;
    }
    ZonedDateTime nextCycle =
        now.plus(Duration.ofMillis((reevaluacionMs + CYCLE_MARGIN_MS) * clock.getTimeFactor()));
    return siteState.site().fee().isRushHour(nextCycle) ? nextCycle : now;
  }

  /** Request decisions from Core and executes it's commands */
  public synchronized void execute() {
    if (!active) {
      logger.debug("Controller is stopped");
      return;
    }

    try {
      ZonedDateTime simulatedDateTime = this.clock.getTimeZoned();

      DataSite siteState = siteStateService.getSiteState(simulatedDateTime);

      turnOffUncontrollableSwitches(simulatedDateTime);

      CoreAnswer coreResponse =
          core.calcularComandos(timeForCore(simulatedDateTime, siteState), siteState);

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
          siteStateService.updateSwitchState(
              commandSwitch.idSwitch(), commandSwitch.estado(), simulatedDateTime);
          logger.info(
              "Switch {} state updated to {}", commandSwitch.idSwitch(), commandSwitch.estado());

        } catch (SwitchClient.SwitchInBackoffException e) {
          logger.debug("Switch {} failed recently, command postponed", commandSwitch.idSwitch());
        } catch (Exception e) {
          logger.error(
              "Error executing a command to the switch {}, with url {}",
              commandSwitch.idSwitch(),
              room.urlSwitch());
        }
      }
    } catch (Exception e) {
      logger.error("Error in the automatic temperature control cycle");
      logger.debug("Error: ", e);
    }
  }

  /**
   * Best effort when the controller is stopped: sends OFF once to every switch not known to be OFF.
   * A switch that fails (or is in backoff) is skipped, never retried, so stopping can't hang.
   */
  private void turnOffAllSwitches() {
    ZonedDateTime now = clock.getTimeZoned();
    List<Room> rooms;
    try {
      rooms = siteStateService.getAllRooms();
    } catch (IllegalStateException e) {
      return; // site not initialized: nothing to turn off
    }
    for (Room room : rooms) {
      if (room.currentSwitchState() == SwitchState.APAGADO) {
        continue;
      }
      try {
        switchClient.apagar(room);
        siteStateService.updateSwitchState(room.idSwitch(), SwitchState.APAGADO, now);
        logger.info("Turned OFF the switch {} because the controller was stopped", room.idSwitch());
      } catch (Exception e) {
        logger.warn("Could not turn OFF the switch {} when stopping", room.idSwitch());
      }
    }
  }

  /**
   * Turns OFF the switches the controller can no longer manage: rooms removed from the config and
   * rooms whose thermostat stopped publishing. A switch is dropped from the pending list only when
   * the OFF succeeded, otherwise it is retried on the next cycle.
   */
  private void turnOffUncontrollableSwitches(ZonedDateTime simulatedDateTime) {
    for (Room orphan : siteStateService.getOrphanRooms()) {
      try {
        switchClient.apagar(orphan);
        siteStateService.removeOrphanRoom(orphan);
        logger.info("Turned OFF the switch {} of a room removed from the site", orphan.idSwitch());
      } catch (SwitchClient.SwitchInBackoffException e) {
        logger.debug("Switch {} failed recently, OFF postponed", orphan.idSwitch());
      } catch (Exception e) {
        logger.error(
            "Error turning OFF the switch {} of a removed room, with url {}",
            orphan.idSwitch(),
            orphan.urlSwitch());
      }
    }
    for (Room stale : siteStateService.getStaleOnRooms()) {
      try {
        switchClient.apagar(stale);
        siteStateService.updateSwitchState(stale.idSwitch(), SwitchState.APAGADO, simulatedDateTime);
        logger.warn(
            "Turned OFF the switch {}: thermostat {} sent no readings recently",
            stale.idSwitch(),
            stale.idThermostat());
      } catch (SwitchClient.SwitchInBackoffException e) {
        logger.debug("Switch {} failed recently, OFF postponed", stale.idSwitch());
      } catch (Exception e) {
        logger.error(
            "Error turning OFF the switch {} of a room without recent readings, with url {}",
            stale.idSwitch(),
            stale.urlSwitch());
      }
    }
  }

  /** Readings outside this range (°C) are sensor faults, not a real room temperature */
  private static final double MIN_PLAUSIBLE_C = -10.0;

  private static final double MAX_PLAUSIBLE_C = 50.0;

  /** @return true when the temperature is a finite value within a plausible range for a room */
  public static boolean isPlausibleTemperature(double tC) {
    return Double.isFinite(tC) && tC >= MIN_PLAUSIBLE_C && tC <= MAX_PLAUSIBLE_C;
  }

  /**
   * Processes a temperature reading message. Updates the site state and executes a control cycle.
   *
   * @param temperatureMsg Parsed temperature reading from MQTT
   */
  public void processTemperature(TemperatureMessage temperatureMsg) {
    if (temperatureMsg.idThermostat() == null
        || temperatureMsg.idThermostat().isBlank()
        || !isPlausibleTemperature(temperatureMsg.tC())) {
      logger.warn(
          "Invalid reading data. Thermostat ID: {}, temperature Cº: {}",
          temperatureMsg.idThermostat(),
          temperatureMsg.tC());
      return;
    }

    if (!active) {
      logger.debug(
          "Controller is stopped, only keeps the temperature sent by thermostat with ID {}",
          temperatureMsg.idThermostat());
      // The in-memory state is kept up to date so it is not outdated when the controller starts
      try {
        siteStateService.updateRoomTemperature(temperatureMsg.idThermostat(), temperatureMsg.tC());
      } catch (RuntimeException e) {
        logger.debug("Reading not kept while stopped: ", e);
      }
      return;
    }

    try {
      siteStateService.updateRoomTemperature(temperatureMsg.idThermostat(), temperatureMsg.tC());
      execute();

    } catch (IllegalArgumentException e) {
      logger.warn(
          "Reading from unknown thermostat ID {} was ignored", temperatureMsg.idThermostat());
    } catch (Exception e) {
      logger.error(
          "Error processing temperature change for thermostat ID {}",
          temperatureMsg.idThermostat());
      logger.debug("Error: ", e);
    }
  }
}
