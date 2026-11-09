package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.SwitchState;
import java.time.ZonedDateTime;

/**
 * @param lastSwitchChange start of the room's current situation (null if unknown): for an ON room,
 *     when its switch was turned ON; for an OFF room, when its switch was turned OFF or when it was
 *     last at/over its expected temperature, whichever is later. Lets the Core know how long a room
 *     has been waiting or running, to prevent starvation.
 */
public record Room(
    String id,
    String name,
    double expectedTemperature,
    double powerKW,
    String idThermostat,
    String topicThermostat,
    String idSwitch,
    String urlSwitch,
    Double currentTemperatureC,
    //        double currentTemperatureF,
    SwitchState currentSwitchState,
    ZonedDateTime lastSwitchChange) {

  /** Copy of this room with a different {@code lastSwitchChange}. */
  public Room withLastSwitchChange(ZonedDateTime newLastSwitchChange) {
    return new Room(
        id,
        name,
        expectedTemperature,
        powerKW,
        idThermostat,
        topicThermostat,
        idSwitch,
        urlSwitch,
        currentTemperatureC,
        currentSwitchState,
        newLastSwitchChange);
  }

  /** Room whose last switch change is unknown. */
  public Room(
      String id,
      String name,
      double expectedTemperature,
      double powerKW,
      String idThermostat,
      String topicThermostat,
      String idSwitch,
      String urlSwitch,
      Double currentTemperatureC,
      SwitchState currentSwitchState) {
    this(
        id,
        name,
        expectedTemperature,
        powerKW,
        idThermostat,
        topicThermostat,
        idSwitch,
        urlSwitch,
        currentTemperatureC,
        currentSwitchState,
        null);
  }
}
