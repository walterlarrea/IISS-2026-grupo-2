package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import com.iiss.sensores.grupo_2.modelos.input.Room;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Keeps a complete in-memory copy of the site state (with temperatures and switch status) */
@Service
public class SiteStateService {
  private DataSite dataSite;
  private final SwitchClient switchClient;
  private final TemperatureReadingRepository tempReadings;

  /** Initialize dirty state as true to trigger a full load of temperatures and switch states */
  private boolean stateMarkedDirty = true;

  public SiteStateService(SwitchClient switchClient, TemperatureReadingRepository tempReadings) {
    this.switchClient = switchClient;
    this.tempReadings = tempReadings;
  }

  /**
   * Initializes or replaces the entire site config state.
   *
   * <p>Side effects: Will hydrate with lastest temperature readings from DB and switch statuses
   * from switches APIs.
   *
   * @param newSiteState the complete new config for the site
   */
  public synchronized void loadSiteState(DataSite newSiteState) {
    if (newSiteState == null
        || newSiteState.site() == null
        || newSiteState.habitaciones() == null) {
      throw new IllegalArgumentException("The Site state cannot be empty nor null");
    }
    this.dataSite = new DataSite(newSiteState.site(), List.copyOf(newSiteState.habitaciones()));
    this.stateMarkedDirty = true;
  }

  private void checkInitializedSiteState() {
    if (dataSite == null) {
      throw new IllegalStateException("The Site state is not yet been initialized");
    }
  }

  private void replaceSiteRooms(List<Room> rooms) {
    dataSite = new DataSite(dataSite.site(), List.copyOf(rooms));
  }

  /**
   * Gets the entire site config state.
   *
   * @return The complete config for the site, with latest temperature readings and switches
   *     statuses
   */
  public synchronized DataSite getSiteState() {
    return getSiteState(null);
  }

  /**
   * Gets the entire site config state, keeping up to date the moment each room's current situation
   * started (see {@link Room#lastSwitchChange()}) so the Core can prevent starvation:
   *
   * <ul>
   *   <li>rooms whose start is unknown are stamped with {@code now}
   *   <li>OFF rooms at or over their expected temperature are not waiting, so their clock restarts
   * </ul>
   *
   * @param now current (simulated) time, or null to skip stamping
   */
  public synchronized DataSite getSiteState(ZonedDateTime now) {
    if (dataSite == null) {
      throw new IllegalStateException("The Site state is not yet been initialized");
    }
    if (stateMarkedDirty) {
      // All thermostat IDs
      List<String> idThermostats =
          dataSite.habitaciones().stream().map(Room::idThermostat).toList();
      // Get latest temp reading for each thermostat ID
      List<TemperatureMessage> latestReadings =
          tempReadings.getLastTemperatureMeasurements(idThermostats);
      List<Room> enrichedRooms =
          dataSite.habitaciones().stream()
              .map(
                  room -> {
                    Double currentTemp =
                        latestReadings.stream()
                            .filter(reading -> reading.idThermostat().equals(room.idThermostat()))
                            .map(TemperatureMessage::tC)
                            .findFirst()
                            .orElse(null);

                    // TODO: Consider handling stale (old) temperature readings

                    return new Room(
                        room.id(),
                        room.name(),
                        room.expectedTemperature(),
                        room.powerKW(),
                        room.idThermostat(),
                        room.topicThermostat(),
                        room.idSwitch(),
                        room.urlSwitch(),
                        currentTemp,
                        switchClient.consultarEstado(room).estado(),
                        room.lastSwitchChange());
                  })
              .toList();
      dataSite = new DataSite(dataSite.site(), List.copyOf(enrichedRooms));
      stateMarkedDirty = false;
    }

    if (now != null) {
      replaceSiteRooms(
          dataSite.habitaciones().stream().map(room -> stampSituationStart(room, now)).toList());
    }

    // Skip rooms with no temperature reading
    return new DataSite(
        dataSite.site(),
        List.copyOf(dataSite.habitaciones()).stream()
            .filter(room -> room.currentTemperatureC() != null)
            .toList());
  }

  private static Room stampSituationStart(Room room, ZonedDateTime now) {
    boolean satisfiedAndOff =
        room.currentSwitchState() == SwitchState.APAGADO
            && room.currentTemperatureC() != null
            && room.currentTemperatureC() >= room.expectedTemperature();
    if (room.lastSwitchChange() == null || satisfiedAndOff) {
      return room.withLastSwitchChange(now);
    }
    return room;
  }

  /** Updates the temperature received from a thermostat */
  public synchronized DataSite updateRoomTemperature(String idThermostat, double temperature) {
    checkInitializedSiteState();
    List<Room> habitaciones = new ArrayList<>(dataSite.habitaciones());
    boolean roomFound = false;

    if (idThermostat == null || idThermostat.isBlank() || !Double.isFinite(temperature)) {
      throw new IllegalArgumentException("Invalid thermostat ID or temperature");
    }

    for (int i = 0; i < habitaciones.size(); i++) {
      Room h = habitaciones.get(i);
      if (h.idThermostat().equals(idThermostat)) {
        habitaciones.set(
            i,
            new Room(
                h.id(),
                h.name(),
                h.expectedTemperature(),
                h.powerKW(),
                h.idThermostat(),
                h.topicThermostat(),
                h.idSwitch(),
                h.urlSwitch(),
                temperature,
                h.currentSwitchState(),
                h.lastSwitchChange()));
        roomFound = true;
      }
    }

    if (!roomFound) {
      throw new IllegalArgumentException("No Room associated with the thermostat: " + idThermostat);
    }
    replaceSiteRooms(habitaciones);
    return dataSite;
  }

  /** Updates known state of a switch, keeping its situation start unless the state changed */
  public synchronized DataSite updateSwitchState(String idSwitch, SwitchState newSwitchState) {
    return updateSwitchState(idSwitch, newSwitchState, null);
  }

  /**
   * Updates known state of a switch.
   *
   * @param changedAt (simulated) moment of the change; when the state actually changes it becomes
   *     the room's {@code lastSwitchChange} (null leaves it unknown)
   */
  public synchronized DataSite updateSwitchState(
      String idSwitch, SwitchState newSwitchState, ZonedDateTime changedAt) {
    checkInitializedSiteState();
    List<Room> rooms = new ArrayList<>(dataSite.habitaciones());
    boolean roomFound = false;

    if (idSwitch == null || idSwitch.isBlank() || newSwitchState == null) {
      throw new IllegalArgumentException("Invalid switch ID or new switch state");
    }

    for (int i = 0; i < rooms.size(); i++) {
      Room h = rooms.get(i);
      if (h.idSwitch().equals(idSwitch)) {
        rooms.set(
            i,
            new Room(
                h.id(),
                h.name(),
                h.expectedTemperature(),
                h.powerKW(),
                h.idThermostat(),
                h.topicThermostat(),
                h.idSwitch(),
                h.urlSwitch(),
                h.currentTemperatureC(),
                newSwitchState,
                h.currentSwitchState() == newSwitchState ? h.lastSwitchChange() : changedAt));
        roomFound = true;
        break;
      }
    }

    if (!roomFound) {
      throw new IllegalArgumentException("No Room associated with the switch:  " + idSwitch);
    }
    replaceSiteRooms(rooms);
    return dataSite;
  }
}
