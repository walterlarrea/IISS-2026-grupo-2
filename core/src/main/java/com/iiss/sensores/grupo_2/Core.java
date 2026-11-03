package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import com.iiss.sensores.grupo_2.modelos.input.Room;
import com.iiss.sensores.grupo_2.modelos.output.CommandSwitch;
import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;
import com.iiss.sensores.grupo_2.modelos.output.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Logic to make decisions based on site data config, temperature readings and domain requirements.
 * Handles power limitations, underfloor heating thermal inertia, and prevents room starvation.
 */
class Core implements ICore {
  private static final Logger logger = LoggerFactory.getLogger(Core.class);

  public static final double DEFAULT_THERMAL_INERTIA_BAND_C = 0.5;
  public static final Duration DEFAULT_MAX_WAIT = Duration.ofMinutes(60);
  public static final Duration DEFAULT_MIN_ON_TIME = Duration.ofMinutes(10);

  private final double thermalInertiaBandC;
  private final Duration maxWait;
  private final Duration minOnTime;

  /** Default constructor using the default thermal inertia band, max wait and min ON time. */
  public Core() {
    this(DEFAULT_THERMAL_INERTIA_BAND_C);
  }

  /**
   * Constructor allowing configurable thermal inertia tolerance.
   *
   * @param thermalInertiaBandC tolerance band in °C for early cutoff / anti-starvation.
   */
  public Core(double thermalInertiaBandC) {
    this(thermalInertiaBandC, DEFAULT_MAX_WAIT, DEFAULT_MIN_ON_TIME);
  }

  /**
   * Fully parameterized constructor.
   *
   * @param thermalInertiaBandC tolerance band in °C for early cutoff.
   * @param maxWait how long a room below its expected temperature may wait with its switch OFF
   *     before it is promoted over any other room, even preempting rooms that are already ON.
   * @param minOnTime how long a room must stay ON before it can be preempted or yield its power.
   */
  public Core(double thermalInertiaBandC, Duration maxWait, Duration minOnTime) {
    this.thermalInertiaBandC = thermalInertiaBandC;
    this.maxWait = maxWait;
    this.minOnTime = minOnTime;
  }

  /**
   * Calculates the switch actuation commands required to maintain room temperatures within limits
   * without exceeding the contracted power capacity of the site.
   *
   * @param dateTime current simulated timestamp used to determine tariff periods (e.g. rush hour).
   * @param siteData current site configuration and status of all rooms.
   * @return {@link CoreAnswer} containing the list of switch commands to be dispatched.
   */
  @Override
  public CoreAnswer calcularComandos(ZonedDateTime dateTime, DataSite siteData) {
    logger.debug(
        "Calculating commands for {}. Received site config.: {}", dateTime, siteData != null);
    if (siteData == null || siteData.habitaciones() == null || siteData.habitaciones().isEmpty()) {
      logger.debug("No rooms found for evaluation; Not generating commands");
      return new CoreAnswer(List.of());
    }
    logger.debug("Evaluating {} rooms", siteData.habitaciones().size());

    /** Turn OFF all switches during the configured rush hour period */
    if (siteData.site() != null
        && siteData.site().fee() != null
        && siteData.site().fee().isRushHour(dateTime)) {
      logger.debug("Rush hour period active; all switches currently turned ON will be turned OFF");
      List<Command> commands = new ArrayList<>();
      for (Room room : siteData.habitaciones()) {
        appendCommandIfNecessary(commands, room, SwitchState.APAGADO);
      }
      logger.debug("Finished calculating rush hour: {} commands", commands.size());
      return new CoreAnswer(commands);
    }

    /** Set of rooms at or over expected temperature */
    List<Room> roomsOverTheExpectedTemperature =
        siteData.habitaciones().stream()
            .filter(room -> room.currentTemperatureC() >= room.expectedTemperature())
            .toList();

    /** Set of rooms below their expected temperature, ordered by temperature deficit DESC */
    List<Room> roomsBelowExpectedTemperature =
        siteData.habitaciones().stream()
            .filter(room -> room.currentTemperatureC() < room.expectedTemperature())
            .sorted(
                Comparator.comparingDouble(
                        (Room room) -> room.expectedTemperature() - room.currentTemperatureC())
                    .reversed())
            .toList();
    logger.debug(
        "Rooms at or over the expected temperature: {}; Below the expected temperature: {}",
        roomsOverTheExpectedTemperature.size(),
        roomsBelowExpectedTemperature.size());

    /** Calculate initial power used by rooms whose switch is turned ON */
    double utilizedPower =
        siteData.habitaciones().stream()
            .filter(room -> room.currentSwitchState() == SwitchState.ENCENDIDO)
            .mapToDouble(Room::powerKW)
            .sum();
    logger.debug("Power utilized initially: {} kW", utilizedPower);

    List<Command> commands = new ArrayList<>();
    Set<String> switchesTurnedOff = new HashSet<>();
    Set<String> switchesTurnedOn = new HashSet<>();
    double contractedPower = (siteData.site() != null) ? siteData.site().potenciaContratadaKW() : 0.0;

    /**
     * 1. Turn OFF switches of rooms that reached or exceeded their expected temperature.
     * Frees power before attempting to turn ON switches for rooms that are below expected temperature.
     */
    for (Room room : roomsOverTheExpectedTemperature) {
      if (room.currentSwitchState() != SwitchState.APAGADO) {
        commands.add(new CommandSwitch(room.idSwitch(), SwitchState.APAGADO));
        switchesTurnedOff.add(room.idSwitch());
        utilizedPower -= room.powerKW();
        logger.debug(
            "Turning OFF switch {}: current temperature {} °C, expected {} °C; power freed {} kW",
            room.idSwitch(),
            room.currentTemperatureC(),
            room.expectedTemperature(),
            room.powerKW());
      }
    }

    /**
     * 2. Anti-starvation by waiting time:
     * Rooms below their expected temperature that have waited OFF for maxWait or longer are served
     * first (longest wait first), regardless of their deficit. If there isn't enough free power,
     * rooms already ON (past their minimum ON time) are turned OFF, lowest deficit first, but only
     * when that frees enough power for the starved room to actually fit.
     */
    List<Room> starvedRooms =
        roomsBelowExpectedTemperature.stream()
            .filter(room -> isStarved(room, dateTime))
            .sorted(Comparator.comparing((Room room) -> elapsedSince(room, dateTime)).reversed())
            .toList();
    for (Room starved : starvedRooms) {
      double missingPower = utilizedPower + starved.powerKW() - contractedPower;
      List<Room> victims = new ArrayList<>();
      if (missingPower > 0) {
        List<Room> preemptable =
            siteData.habitaciones().stream()
                .filter(room -> room.currentSwitchState() == SwitchState.ENCENDIDO)
                .filter(room -> !switchesTurnedOff.contains(room.idSwitch()))
                .filter(room -> !isProtectedByMinOnTime(room, dateTime))
                .sorted(
                    Comparator.comparingDouble(
                        (Room room) -> room.expectedTemperature() - room.currentTemperatureC()))
                .toList();
        double freedPower = 0.0;
        for (Room candidate : preemptable) {
          if (freedPower >= missingPower) {
            break;
          }
          victims.add(candidate);
          freedPower += candidate.powerKW();
        }
        if (freedPower < missingPower) {
          logger.debug(
              "Starved room {} cannot be served: preemptable rooms free {} kW but {} kW are missing",
              starved.idSwitch(),
              freedPower,
              missingPower);
          continue;
        }
      }
      for (Room victim : victims) {
        commands.add(new CommandSwitch(victim.idSwitch(), SwitchState.APAGADO));
        switchesTurnedOff.add(victim.idSwitch());
        utilizedPower -= victim.powerKW();
        logger.debug(
            "Preempting switch {} (ON for {}) to serve starved room {}",
            victim.idSwitch(),
            elapsedSince(victim, dateTime),
            starved.idSwitch());
      }
      commands.add(new CommandSwitch(starved.idSwitch(), SwitchState.ENCENDIDO));
      switchesTurnedOn.add(starved.idSwitch());
      utilizedPower += starved.powerKW();
      logger.debug(
          "Turning ON switch {}: starved after waiting {}; power utilized {}/{} kW",
          starved.idSwitch(),
          elapsedSince(starved, dateTime),
          utilizedPower,
          contractedPower);
    }

    /**
     * 3. Thermal Inertia early release:
     * When there are colder rooms awaiting power that exceed the available capacity, active rooms
     * already inside their thermal inertia band (within thermalInertiaBandC of their target)
     * yield their switch to OFF. This allows the colder rooms to receive power and heat up.
     */
    for (Room activeRoom : siteData.habitaciones()) {
      if (activeRoom.currentSwitchState() == SwitchState.ENCENDIDO
          && !switchesTurnedOff.contains(activeRoom.idSwitch())
          && !isProtectedByMinOnTime(activeRoom, dateTime)) {
        boolean isInInertiaBand = activeRoom.currentTemperatureC() >= (activeRoom.expectedTemperature() - thermalInertiaBandC);
        double activeDeficit = activeRoom.expectedTemperature() - activeRoom.currentTemperatureC();

        // Check if there is any colder waiting room that cannot turn ON without more power
        boolean colderRoomWaiting = false;
        for (Room waiting : roomsBelowExpectedTemperature) {
          if (waiting.currentSwitchState() == SwitchState.APAGADO
              && !switchesTurnedOn.contains(waiting.idSwitch())) {
            double waitingDeficit = waiting.expectedTemperature() - waiting.currentTemperatureC();
            if (waitingDeficit > activeDeficit && (utilizedPower + waiting.powerKW() > contractedPower)) {
              colderRoomWaiting = true;
              break;
            }
          }
        }

        if (isInInertiaBand && colderRoomWaiting) {
          commands.add(new CommandSwitch(activeRoom.idSwitch(), SwitchState.APAGADO));
          switchesTurnedOff.add(activeRoom.idSwitch());
          utilizedPower -= activeRoom.powerKW();
          logger.debug(
              "Yielding power due to thermal inertia / anti-starvation: turning OFF switch {}. "
                  + "Temp: {} °C (Target: {} °C), freed: {} kW",
              activeRoom.idSwitch(),
              activeRoom.currentTemperatureC(),
              activeRoom.expectedTemperature(),
              activeRoom.powerKW());
        }
      }
    }

    /**
     * 4. Turn ON switches of rooms that are below expected temperature in descending order of deficit.
     * Switches are turned ON only if enough power is available.
     */
    for (Room room : roomsBelowExpectedTemperature) {
      boolean alreadyHandledInThisCycle =
          switchesTurnedOff.contains(room.idSwitch()) || switchesTurnedOn.contains(room.idSwitch());

      if (room.currentSwitchState() != SwitchState.ENCENDIDO && !alreadyHandledInThisCycle) {
        /** Room is turned ON only when it won't surpass the available power. */
        if (utilizedPower + room.powerKW() <= contractedPower) {
          commands.add(new CommandSwitch(room.idSwitch(), SwitchState.ENCENDIDO));
          utilizedPower += room.powerKW();
          logger.debug(
              "Turning ON switch {}: temperature deficit {} °C; power utilized {}/{} kW",
              room.idSwitch(),
              room.expectedTemperature() - room.currentTemperatureC(),
              utilizedPower,
              contractedPower);
        } else {
          logger.debug(
              "Can't turn ON switch {}: required power {} kW exceeds the established limit of {} kW",
              room.idSwitch(),
              utilizedPower + room.powerKW(),
              contractedPower);
        }
      }
    }

    logger.debug(
        "Finished calculating commands: {} commands; final utilized power: {} kW",
        commands.size(),
        utilizedPower);
    return new CoreAnswer(commands);
  }

  /**
   * Time elapsed since the room's current situation started (its switch last changed state or, for
   * a satisfied room, it was last satisfied).
   *
   * @return the elapsed time, or null when unknown.
   */
  private static Duration elapsedSince(Room room, ZonedDateTime now) {
    if (room.lastSwitchChange() == null) {
      return null;
    }
    Duration elapsed = Duration.between(room.lastSwitchChange(), now);
    return elapsed.isNegative() ? Duration.ZERO : elapsed;
  }

  /** A room is starved when it is OFF, below its expected temperature and waited maxWait or more. */
  private boolean isStarved(Room room, ZonedDateTime now) {
    Duration elapsed = elapsedSince(room, now);
    return room.currentSwitchState() == SwitchState.APAGADO
        && elapsed != null
        && elapsed.compareTo(maxWait) >= 0;
  }

  /** A room just turned ON must stay ON for minOnTime. Unknown times are not protected. */
  private boolean isProtectedByMinOnTime(Room room, ZonedDateTime now) {
    Duration elapsed = elapsedSince(room, now);
    return elapsed != null && elapsed.compareTo(minOnTime) < 0;
  }

  /**
   * Helper method to append a switch command if the switch is not already in the target state.
   *
   * @param commands list of commands to append to.
   * @param room room whose switch is being evaluated.
   * @param newStatus desired state for the switch.
   */
  private void appendCommandIfNecessary(List<Command> commands, Room room, SwitchState newStatus) {
    if (room.currentSwitchState() != newStatus) {
      commands.add(new CommandSwitch(room.idSwitch(), newStatus));
      logger.debug("Adding command for switch {}: status {}", room.idSwitch(), newStatus);
    } else {
      logger.debug(
          "Switch {} is already on status {}; not generating a command",
          room.idSwitch(),
          newStatus);
    }
  }
}
