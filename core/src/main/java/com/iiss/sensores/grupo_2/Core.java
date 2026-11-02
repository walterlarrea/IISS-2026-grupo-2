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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Logic to make decisions based on site data config, temperature readings and domain requirements.
 * Handles power limitations, underfloor heating thermal inertia, and prevents room starvation.
 */
class Core implements ICore {
  private static final Logger logger = LoggerFactory.getLogger(Core.class);

  /**
   * Thermal inertia tolerance band in °C.
   * Underfloor heating stores significant heat in the slab. When a room is within this band of
   * its expected temperature and other colder rooms are awaiting power, the switch can be turned
   * off early, relying on accumulated thermal inertia to reach the target while releasing
   * electrical capacity for other rooms to avoid starvation.
   */
  public static final double THERMAL_INERTIA_BAND_C = 0.5;

  public Core() {}

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

    /**
     * 1. Turn OFF switches of rooms that reached or exceeded their expected temperature.
     * Frees power before attempting to turn ON switches for rooms that are below expected temperature.
     */
    for (Room room : roomsOverTheExpectedTemperature) {
      if (room.currentSwitchState() != SwitchState.APAGADO) {
        commands.add(new CommandSwitch(room.idSwitch(), SwitchState.APAGADO));
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
     * 2. Anti-starvation & Thermal Inertia early release:
     * When there are colder rooms awaiting power that exceed the available capacity, active rooms
     * already inside their thermal inertia band (within THERMAL_INERTIA_BAND_C of their target)
     * yield their switch to OFF. This allows the colder rooms to receive power and heat up.
     */
    double contractedPower = (siteData.site() != null) ? siteData.site().potenciaContratadaKW() : 0.0;
    for (Room activeRoom : siteData.habitaciones()) {
      if (activeRoom.currentSwitchState() == SwitchState.ENCENDIDO) {
        boolean isInInertiaBand = activeRoom.currentTemperatureC() >= (activeRoom.expectedTemperature() - THERMAL_INERTIA_BAND_C);
        double activeDeficit = activeRoom.expectedTemperature() - activeRoom.currentTemperatureC();

        // Check if there is any colder waiting room that cannot turn ON without more power
        boolean colderRoomWaiting = false;
        for (Room waiting : roomsBelowExpectedTemperature) {
          if (waiting.currentSwitchState() == SwitchState.APAGADO) {
            double waitingDeficit = waiting.expectedTemperature() - waiting.currentTemperatureC();
            if (waitingDeficit > activeDeficit && (utilizedPower + waiting.powerKW() > contractedPower)) {
              colderRoomWaiting = true;
              break;
            }
          }
        }

        if (isInInertiaBand && colderRoomWaiting) {
          commands.add(new CommandSwitch(activeRoom.idSwitch(), SwitchState.APAGADO));
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
     * 3. Turn ON switches of rooms that are below expected temperature in descending order of deficit.
     * Switches are turned ON only if enough power is available.
     */
    for (Room room : roomsBelowExpectedTemperature) {
      boolean alreadyTurnedOffInThisCycle = commands.stream()
          .anyMatch(c -> c instanceof CommandSwitch cs
              && cs.idSwitch().equals(room.idSwitch())
              && cs.estado() == SwitchState.APAGADO);

      if (room.currentSwitchState() != SwitchState.ENCENDIDO && !alreadyTurnedOffInThisCycle) {
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
