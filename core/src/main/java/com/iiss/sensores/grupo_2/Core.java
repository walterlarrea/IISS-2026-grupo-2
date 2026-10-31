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
 * Logic to take decisions based on site data config, temperature readings and domain requirements
 */
class Core implements ICore {
  private static final Logger logger = LoggerFactory.getLogger(Core.class);

  public Core() {}

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

    /** Calculate power used by rooms whose switch is turned ON */
    double utilizedPower =
        siteData.habitaciones().stream()
            .filter(room -> room.currentSwitchState() == SwitchState.ENCENDIDO)
            .mapToDouble(Room::powerKW)
            .sum();
    logger.debug("Power utilized initially: {} kW", utilizedPower);

    List<Command> commands = new ArrayList<>();

    /**
     * - Appends the commands for turning OFF switches of rooms that are at or over their expected
     * temperature and are currently ON. - Frees power before trying to turn ON switches for rooms
     * that are below expected temperature
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
     * - Appends the commands for turning ON the switches of rooms that are below their expected
     * temperature and are currently OFF. - Switches are turned ON only if enough power is
     * available. - If the room with greater deficit can't be turned ON because of lack of power,
     * the system attempts to turn ON the next room in order (deficit DESC).
     */
    for (Room room : roomsBelowExpectedTemperature) {
      if (room.currentSwitchState() != SwitchState.ENCENDIDO) {
        assert siteData.site() != null;
        /** Room is turned ON only when it won't surpass the available power. */
        if (utilizedPower + room.powerKW() <= siteData.site().potenciaContratadaKW()) {
          commands.add(new CommandSwitch(room.idSwitch(), SwitchState.ENCENDIDO));
          utilizedPower += room.powerKW();
          logger.debug(
              "Turning ON switch {}: temperature deficit {} °C; power utilized {}/{} kW",
              room.idSwitch(),
              room.expectedTemperature() - room.currentTemperatureC(),
              utilizedPower,
              siteData.site().potenciaContratadaKW());
        } else {
          logger.debug(
              "Can't turn ON switch {}: required power {} kW exceeds the established limit of {} kW",
              room.idSwitch(),
              utilizedPower + room.powerKW(),
              siteData.site().potenciaContratadaKW());
        }
      }
    }

    logger.debug(
        "Finished calculating commands: {} commands; final utilized power: {} kW",
        commands.size(),
        utilizedPower);
    return new CoreAnswer(commands);
  }

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
