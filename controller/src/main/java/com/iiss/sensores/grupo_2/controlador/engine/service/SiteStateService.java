package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import com.iiss.sensores.grupo_2.modelos.input.Room;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class SiteStateService {
  // conserva el estado actual en memoria, actualiza las temperaturas y registra los estados de los
  // switches
  private DataSite dataSite;
  private final SwitchClient switchClient;
  private final TemperatureReadingRepository tempReadings;
  private boolean stateMarkedDirty;

  public SiteStateService(SwitchClient switchClient, TemperatureReadingRepository tempReadings) {
    this.switchClient = switchClient;
    this.tempReadings = tempReadings;
  }

  // inicializa o reemplaza el estado del site, ejecutar:
  // estadoSitioService.cargarEstado(dataSitioInicial);
  public synchronized void loadSiteState(DataSite nuevoEstado) {
    if (nuevoEstado == null || nuevoEstado.site() == null || nuevoEstado.habitaciones() == null) {
      throw new IllegalArgumentException("The Site state cannot be empty nor null");
    }
    this.dataSite = new DataSite(nuevoEstado.site(), List.copyOf(nuevoEstado.habitaciones()));
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

  // devuelve el estado actual del site
  public synchronized DataSite getSiteState() {
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
                        switchClient.consultarEstado(room).estado());
                  })
              .toList();
      dataSite = new DataSite(dataSite.site(), List.copyOf(enrichedRooms));
      stateMarkedDirty = false;
    }

    // Skip rooms with no temperature reading
    return new DataSite(
        dataSite.site(),
        List.copyOf(dataSite.habitaciones()).stream()
            .filter(room -> room.currentTemperatureC() != null)
            .toList());
  }

  // actualiza la temperatura recibida de un termostato
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
                h.currentSwitchState()));
        roomFound = true;
      }
    }

    if (!roomFound) {
      throw new IllegalArgumentException("No Room associated with the thermostat: " + idThermostat);
    }
    replaceSiteRooms(habitaciones);
    return dataSite;
  }

  // actualiza el estado conocido de un switch
  public synchronized DataSite updateSwitchState(String idSwitch, SwitchState newSwitchState) {
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
                newSwitchState));
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
