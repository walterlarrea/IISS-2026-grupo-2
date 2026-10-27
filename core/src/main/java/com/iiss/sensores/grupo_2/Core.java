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

class Core implements ICore {
  private static final Logger logger = LoggerFactory.getLogger(Core.class);

  public Core() {
    // Constructor de la clase Core
  }

  @Override
  public CoreAnswer calcularComandos(ZonedDateTime dateTime, DataSite siteData) {
    logger.debug("Calculando commands para {}. Site recibido: {}", dateTime, siteData != null);
    if (siteData == null || siteData.habitaciones() == null || siteData.habitaciones().isEmpty()) {
      logger.debug("No hay habitaciones para evaluar; no se generan commands");
      return new CoreAnswer(List.of());
    }
    logger.debug("Se evaluarán {} habitaciones", siteData.habitaciones().size());

    // Apaga todos los switches durante el horario rushHour
    if (siteData.site() != null
        && siteData.site().fee() != null
        && siteData.site().fee().isRushHour(dateTime)) {
      logger.debug("Horario rushHour activo; se apagarán los switches que estén encendidos");
      List<Command> commands = new ArrayList<>();
      for (Room room : siteData.habitaciones()) {
        agregarComandoSiNecesario(commands, room, SwitchState.APAGADO);
      }
      logger.debug("Cálculo en horario rushHour finalizado: {} commands", commands.size());
      return new CoreAnswer(commands);
    }

    // Lista de habitaciones que alcanzaron o están por encima de la temperatura esperada
    List<Room> habitacionesSobreTemperaturaEsperada =
        siteData.habitaciones().stream()
            .filter(
                habitacion -> habitacion.currentTemperatureC() >= habitacion.expectedTemperature())
            .toList();

    // Lista de habitaciones que están por debajo de la temperatura esperada,
    // ordenadas por la diferencia entre la temperatura esperada y la actual (de mayor a menor)
    List<Room> habitacionesBajoTemperaturaEsperada =
        siteData.habitaciones().stream()
            .filter(
                habitacion -> habitacion.currentTemperatureC() < habitacion.expectedTemperature())
            .sorted(
                Comparator.comparingDouble(
                        (Room habitacion) ->
                            habitacion.expectedTemperature() - habitacion.currentTemperatureC())
                    .reversed())
            .toList();
    logger.debug(
        "Habitaciones en o sobre temperatura esperada: {}; bajo temperatura esperada: {}",
        habitacionesSobreTemperaturaEsperada.size(),
        habitacionesBajoTemperaturaEsperada.size());

    // Calcula la potencia utilizada sumando la potencia de las habitaciones cuyo switch está
    // encendido
    double potenciaUtilizada =
        siteData.habitaciones().stream()
            .filter(habitacion -> habitacion.currentSwitchState() == SwitchState.ENCENDIDO)
            .mapToDouble(Room::powerKW)
            .sum();
    logger.debug("Potencia utilizada inicialmente: {} kW", potenciaUtilizada);

    List<Command> commands = new ArrayList<>();

    /* - Agrega los commands para apagar los switches de las habitaciones que están por encima de la temperatura esperada y está encendido
     * -Libera potencia para poder encender las habitaciones que están por debajo de la temperatura esperada
     */
    for (Room room : habitacionesSobreTemperaturaEsperada) {
      if (room.currentSwitchState() != SwitchState.APAGADO) {
        commands.add(new CommandSwitch(room.idSwitch(), SwitchState.APAGADO));
        potenciaUtilizada -= room.powerKW();
        logger.debug(
            "Se apaga el switch {}: temperatura actual {} °C, esperada {} °C; potencia liberada {} kW",
            room.idSwitch(),
            room.currentTemperatureC(),
            room.expectedTemperature(),
            room.powerKW());
      }
    }

    /* - Agrega los commands para encender los switches de las habitaciones que están por debajo de la temperatura esperada y cuyo switch está apagado.
     * - Solo enciende la habitación si no supera la potencia contratada.
     * - Si la habitación con mayor déficit de temperatura no puede encenderse por superar la potencia contratada,
     *     se intenta encender la siguiente habitación con menor déficit de temperatura, y así sucesivamente.
     */
    for (Room room : habitacionesBajoTemperaturaEsperada) {
      if (room.currentSwitchState() != SwitchState.ENCENDIDO) {
        assert siteData.site() != null;
        // Solo enciende la habitación si no supera la potencia contratada
        if (potenciaUtilizada + room.powerKW() <= siteData.site().potenciaContratadaKW()) {
          commands.add(new CommandSwitch(room.idSwitch(), SwitchState.ENCENDIDO));
          potenciaUtilizada += room.powerKW();
          logger.debug(
              "Se enciende el switch {}: déficit de temperatura {} °C; potencia utilizada {}/{} kW",
              room.idSwitch(),
              room.expectedTemperature() - room.currentTemperatureC(),
              potenciaUtilizada,
              siteData.site().potenciaContratadaKW());
        } else {
          logger.debug(
              "No se enciende el switch {}: potencia requerida {} kW supera el límite contratado de {} kW",
              room.idSwitch(),
              potenciaUtilizada + room.powerKW(),
              siteData.site().potenciaContratadaKW());
        }
      }
    }

    logger.debug(
        "Cálculo finalizado: {} commands; potencia utilizada final: {} kW",
        commands.size(),
        potenciaUtilizada);
    return new CoreAnswer(commands);
  }

  private void agregarComandoSiNecesario(
      List<Command> commands, Room room, SwitchState estadoDeseado) {
    if (room.currentSwitchState() != estadoDeseado) {
      commands.add(new CommandSwitch(room.idSwitch(), estadoDeseado));
      logger.debug(
          "Se agrega comando para el switch {}: estado {}", room.idSwitch(), estadoDeseado);
    } else {
      logger.debug(
          "El switch {} ya está en estado {}; no se genera comando",
          room.idSwitch(),
          estadoDeseado);
    }
  }
}
