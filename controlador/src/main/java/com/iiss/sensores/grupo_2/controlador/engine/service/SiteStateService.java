package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class SiteStateService {
  // conserva el estado actual en memoria, actualiza las temperaturas y registra los estados de los
  // switches
  private DataSitio dataSitio;
  private final SwitchClient switchClient;
  private final TemperatureReadingRepository tempReadings;
  private boolean stateMarkedDirty;

  public SiteStateService(SwitchClient switchClient, TemperatureReadingRepository tempReadings) {
    this.switchClient = switchClient;
    this.tempReadings = tempReadings;
  }

  // inicializa o reemplaza el estado del sitio, ejecutar:
  // estadoSitioService.cargarEstado(dataSitioInicial);
  public synchronized void loadSiteState(DataSitio nuevoEstado) {
    if (nuevoEstado == null || nuevoEstado.sitio() == null || nuevoEstado.habitaciones() == null) {
      throw new IllegalArgumentException("The Site state cannot be empty nor null");
    }
    this.dataSitio = new DataSitio(nuevoEstado.sitio(), List.copyOf(nuevoEstado.habitaciones()));
    this.stateMarkedDirty = true;
  }

  private void checkInitializedSiteState() {
    if (dataSitio == null) {
      throw new IllegalStateException("The Site state is not yet been initialized");
    }
  }

  private void replaceSiteRooms(List<Habitacion> rooms) {
    dataSitio = new DataSitio(dataSitio.sitio(), List.copyOf(rooms));
  }

  // devuelve el estado actual del sitio
  public synchronized DataSitio getSiteState() {
    if (dataSitio == null) {
      throw new IllegalStateException("The Site state is not yet been initialized");
    }
    if (stateMarkedDirty) {
      // All thermostat IDs
      List<String> thermostatIds =
          dataSitio.habitaciones().stream().map(Habitacion::idThermostat).toList();
      // Get latest temp reading for each thermostat ID
      List<TemperatureMessage> latestReadings =
          tempReadings.getLastTemperatureMeasurements(thermostatIds);
      List<Habitacion> enrichedRooms =
          dataSitio.habitaciones().stream()
              .map(
                  room -> {
                    Double currentTemp =
                        latestReadings.stream()
                            .filter(reading -> reading.idThermostat().equals(room.idThermostat()))
                            .map(TemperatureMessage::tC)
                            .findFirst()
                            .orElse(null);

                    // TODO: Consider handling stale (old) temperature readings

                    return new Habitacion(
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
      dataSitio = new DataSitio(dataSitio.sitio(), List.copyOf(enrichedRooms));
      stateMarkedDirty = false;
    }

    // Skip rooms with no temperature reading
    return new DataSitio(
        dataSitio.sitio(),
        List.copyOf(dataSitio.habitaciones()).stream()
            .filter(room -> room.currentTemperatureC() != null)
            .toList());
  }

  // actualiza la temperatura recibida de un termostato
  public synchronized DataSitio updateRoomTemperature(String idThermostat, double temperature) {
    checkInitializedSiteState();
    List<Habitacion> habitaciones = new ArrayList<>(dataSitio.habitaciones());
    boolean roomFound = false;

    if (idThermostat == null || idThermostat.isBlank() || !Double.isFinite(temperature)) {
      throw new IllegalArgumentException("Invalid thermostat ID or temperature");
    }

    for (int i = 0; i < habitaciones.size(); i++) {
      Habitacion h = habitaciones.get(i);
      if (h.idThermostat().equals(idThermostat)) {
        habitaciones.set(
            i,
            new Habitacion(
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
    return dataSitio;
  }

  // actualiza el estado conocido de un switch
  public synchronized DataSitio updateSwitchState(String idSwitch, EstadoSwitch newSwitchState) {
    checkInitializedSiteState();
    List<Habitacion> rooms = new ArrayList<>(dataSitio.habitaciones());
    boolean roomFound = false;

    if (idSwitch == null || idSwitch.isBlank() || newSwitchState == null) {
      throw new IllegalArgumentException("Invalid switch ID or new switch state");
    }

    for (int i = 0; i < rooms.size(); i++) {
      Habitacion h = rooms.get(i);
      if (h.idSwitch().equals(idSwitch)) {
        rooms.set(
            i,
            new Habitacion(
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
    return dataSitio;
  }
}
