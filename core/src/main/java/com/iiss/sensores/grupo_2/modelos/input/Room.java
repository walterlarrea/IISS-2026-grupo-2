package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.SwitchState;
import java.time.ZonedDateTime;

/**
 * @param lastSwitchChange moment when the switch last changed its state (null if unknown). Lets the
 *     Core know how long a room has been waiting OFF or running ON, to prevent starvation.
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
