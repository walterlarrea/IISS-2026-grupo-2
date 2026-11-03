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

    if (isRushHour(siteData, dateTime)) {
      return turnOffAllForRushHour(siteData);
    }

    CycleState state = new CycleState(siteData);
    turnOffSatisfiedRooms(state);
    serveStarvedRooms(state, dateTime);
    yieldPowerByThermalInertia(state, dateTime);
    turnOnColdRoomsByDeficit(state);

    logger.debug(
        "Finished calculating commands: {} commands; final utilized power: {} kW",
        state.commands.size(),
        state.utilizedPower);
    return new CoreAnswer(state.commands);
  }

  private static boolean isRushHour(DataSite siteData, ZonedDateTime dateTime) {
    return siteData.site() != null
        && siteData.site().fee() != null
        && siteData.site().fee().isRushHour(dateTime);
  }

  /** Turn OFF all switches during the configured rush hour period. */
  private CoreAnswer turnOffAllForRushHour(DataSite siteData) {
    logger.debug("Rush hour period active; all switches currently turned ON will be turned OFF");
    List<Command> commands = new ArrayList<>();
    for (Room room : siteData.habitaciones()) {
      appendCommandIfNecessary(commands, room, SwitchState.APAGADO);
    }
    logger.debug("Finished calculating rush hour: {} commands", commands.size());
    return new CoreAnswer(commands);
  }

  /**
   * Phase 1: turn OFF switches of rooms that reached or exceeded their expected temperature. Frees
   * power before attempting to turn ON switches for rooms that are below expected temperature.
   */
  private void turnOffSatisfiedRooms(CycleState state) {
    for (Room room : state.roomsAtOrOverExpected) {
      if (room.currentSwitchState() != SwitchState.APAGADO) {
        state.turnOff(room);
        logger.debug(
            "Turning OFF switch {}: current temperature {} °C, expected {} °C; power freed {} kW",
            room.idSwitch(),
            room.currentTemperatureC(),
            room.expectedTemperature(),
            room.powerKW());
      }
    }
  }

  /**
   * Phase 2, anti-starvation by waiting time: rooms below their expected temperature that have
   * waited OFF for maxWait or longer are served first (longest wait first), regardless of their
   * deficit. If there isn't enough free power, rooms already ON (past their minimum ON time) are
   * turned OFF, lowest deficit first, but only when that frees enough power for the starved room to
   * actually fit.
   */
  private void serveStarvedRooms(CycleState state, ZonedDateTime now) {
    List<Room> starvedRooms =
        state.roomsBelowExpected.stream()
            .filter(room -> isStarved(room, now))
            // Starved rooms always have a known wait, so the oldest change is the longest wait.
            .sorted(Comparator.comparing(Room::lastSwitchChange))
            .toList();
    if (starvedRooms.isEmpty()) {
      return;
    }

    // The preemption order does not change during the cycle: sort once, skip rooms already OFF.
    List<Room> preemptable = preemptableRooms(state, now);
    for (Room starved : starvedRooms) {
      List<Room> victims = selectVictims(state, preemptable, starved);
      if (victims == null) {
        continue;
      }
      for (Room victim : victims) {
        state.turnOff(victim);
        if (logger.isDebugEnabled()) {
          logger.debug(
              "Preempting switch {} (ON for {}) to serve starved room {}",
              victim.idSwitch(),
              elapsedSince(victim, now),
              starved.idSwitch());
        }
      }
      state.turnOn(starved);
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Turning ON switch {}: starved after waiting {}; power utilized {}/{} kW",
            starved.idSwitch(),
            elapsedSince(starved, now),
            state.utilizedPower,
            state.contractedPower);
      }
    }
  }

  /** Rooms currently ON and past their minimum ON time, lowest deficit first. */
  private List<Room> preemptableRooms(CycleState state, ZonedDateTime now) {
    return state.rooms.stream()
        .filter(room -> room.currentSwitchState() == SwitchState.ENCENDIDO)
        .filter(room -> !isProtectedByMinOnTime(room, now))
        .sorted(Comparator.comparingDouble(Core::deficit))
        .toList();
  }

  /**
   * Chooses which rooms must be turned OFF to fit the starved room.
   *
   * @return the victims (empty when the room already fits) or null when it cannot be served.
   */
  private List<Room> selectVictims(CycleState state, List<Room> preemptable, Room starved) {
    double missingPower = state.utilizedPower + starved.powerKW() - state.contractedPower;
    List<Room> victims = new ArrayList<>();
    if (missingPower <= 0) {
      return victims;
    }
    double freedPower = 0.0;
    for (Room candidate : preemptable) {
      if (freedPower >= missingPower) {
        break;
      }
      if (!state.turnedOff.contains(candidate.idSwitch())) {
        victims.add(candidate);
        freedPower += candidate.powerKW();
      }
    }
    if (freedPower < missingPower) {
      logger.debug(
          "Starved room {} cannot be served: preemptable rooms free {} kW but {} kW are missing",
          starved.idSwitch(),
          freedPower,
          missingPower);
      return null;
    }
    return victims;
  }

  /**
   * Phase 3, thermal inertia early release: for each colder room that is waiting OFF and does not
   * fit in the available capacity (coldest first), active rooms already inside their thermal
   * inertia band (within thermalInertiaBandC of their target) and less cold than it yield their
   * switch to OFF, lowest deficit first. They yield only when together they free enough power for
   * the waiting room to actually fit; otherwise nothing is turned OFF for it.
   */
  private void yieldPowerByThermalInertia(CycleState state, ZonedDateTime now) {
    for (Room waiting : state.roomsBelowExpected) {
      if (waiting.currentSwitchState() != SwitchState.APAGADO
          || state.isHandled(waiting)
          || state.fits(waiting)) {
        continue;
      }
      List<Room> yielding = selectYieldingRooms(state, waiting, now);
      if (yielding == null) {
        continue;
      }
      for (Room activeRoom : yielding) {
        state.turnOff(activeRoom);
        logger.debug(
            "Yielding power due to thermal inertia to colder room {}: turning OFF switch {}. "
                + "Temp: {} °C (Target: {} °C), freed: {} kW",
            waiting.idSwitch(),
            activeRoom.idSwitch(),
            activeRoom.currentTemperatureC(),
            activeRoom.expectedTemperature(),
            activeRoom.powerKW());
      }
    }
  }

  /**
   * Chooses the active rooms that must yield for the waiting room to fit.
   *
   * @return the rooms to turn OFF, or null when they cannot free enough power.
   */
  private List<Room> selectYieldingRooms(CycleState state, Room waiting, ZonedDateTime now) {
    double missingPower = state.utilizedPower + waiting.powerKW() - state.contractedPower;
    double waitingDeficit = deficit(waiting);
    List<Room> candidates =
        state.rooms.stream()
            .filter(room -> room.currentSwitchState() == SwitchState.ENCENDIDO)
            .filter(room -> !state.turnedOff.contains(room.idSwitch()))
            .filter(room -> !isProtectedByMinOnTime(room, now))
            .filter(room -> isInInertiaBand(room) && deficit(room) < waitingDeficit)
            .sorted(Comparator.comparingDouble(Core::deficit))
            .toList();

    List<Room> yielding = new ArrayList<>();
    double freedPower = 0.0;
    for (Room candidate : candidates) {
      if (freedPower >= missingPower) {
        break;
      }
      yielding.add(candidate);
      freedPower += candidate.powerKW();
    }
    if (freedPower < missingPower) {
      logger.debug(
          "Room {} cannot be served by thermal inertia: candidates free {} kW but {} kW are missing",
          waiting.idSwitch(),
          freedPower,
          missingPower);
      return null;
    }
    return yielding;
  }

  private boolean isInInertiaBand(Room room) {
    return room.currentTemperatureC() >= room.expectedTemperature() - thermalInertiaBandC;
  }

  /**
   * Phase 4: turn ON switches of rooms that are below expected temperature in descending order of
   * deficit. Switches are turned ON only if enough power is available.
   */
  private void turnOnColdRoomsByDeficit(CycleState state) {
    for (Room room : state.roomsBelowExpected) {
      if (room.currentSwitchState() == SwitchState.ENCENDIDO || state.isHandled(room)) {
        continue;
      }
      if (state.fits(room)) {
        state.turnOn(room);
        logger.debug(
            "Turning ON switch {}: temperature deficit {} °C; power utilized {}/{} kW",
            room.idSwitch(),
            deficit(room),
            state.utilizedPower,
            state.contractedPower);
      } else {
        logger.debug(
            "Can't turn ON switch {}: required power {} kW exceeds the established limit of {} kW",
            room.idSwitch(),
            state.utilizedPower + room.powerKW(),
            state.contractedPower);
      }
    }
  }

  private static double deficit(Room room) {
    return room.expectedTemperature() - room.currentTemperatureC();
  }

  /** Mutable state of a single calculation cycle; keeps commands, sets and power consistent. */
  private static final class CycleState {
    final List<Room> rooms;
    final List<Room> roomsAtOrOverExpected;

    /** Rooms below their expected temperature, ordered by temperature deficit DESC. */
    final List<Room> roomsBelowExpected;

    final List<Command> commands = new ArrayList<>();
    final Set<String> turnedOff = new HashSet<>();
    final Set<String> turnedOn = new HashSet<>();
    final double contractedPower;
    double utilizedPower;

    CycleState(DataSite siteData) {
      rooms = siteData.habitaciones();
      roomsAtOrOverExpected =
          rooms.stream()
              .filter(room -> room.currentTemperatureC() >= room.expectedTemperature())
              .toList();
      roomsBelowExpected =
          rooms.stream()
              .filter(room -> room.currentTemperatureC() < room.expectedTemperature())
              .sorted(Comparator.comparingDouble(Core::deficit).reversed())
              .toList();
      utilizedPower =
          rooms.stream()
              .filter(room -> room.currentSwitchState() == SwitchState.ENCENDIDO)
              .mapToDouble(Room::powerKW)
              .sum();
      contractedPower = siteData.site() != null ? siteData.site().potenciaContratadaKW() : 0.0;
      logger.debug(
          "Rooms at or over the expected temperature: {}; Below the expected temperature: {}",
          roomsAtOrOverExpected.size(),
          roomsBelowExpected.size());
      logger.debug("Power utilized initially: {} kW", utilizedPower);
    }

    void turnOff(Room room) {
      commands.add(new CommandSwitch(room.idSwitch(), SwitchState.APAGADO));
      turnedOff.add(room.idSwitch());
      utilizedPower -= room.powerKW();
    }

    void turnOn(Room room) {
      commands.add(new CommandSwitch(room.idSwitch(), SwitchState.ENCENDIDO));
      turnedOn.add(room.idSwitch());
      utilizedPower += room.powerKW();
    }

    boolean fits(Room room) {
      return utilizedPower + room.powerKW() <= contractedPower;
    }

    boolean isHandled(Room room) {
      return turnedOff.contains(room.idSwitch()) || turnedOn.contains(room.idSwitch());
    }
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

  /**
   * A room is starved when it is OFF, below its expected temperature and waited maxWait or more.
   */
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
