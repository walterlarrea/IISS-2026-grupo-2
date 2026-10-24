package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.output.Comando;
import com.iiss.sensores.grupo_2.modelos.output.ComandoSwitch;
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
    public RespuestaCore calcularComandos(ZonedDateTime fechaHora, DataSitio dataSitio) {
        logger.debug("Calculando comandos para {}. Sitio recibido: {}", fechaHora, dataSitio != null);
        if (dataSitio == null || dataSitio.habitaciones() == null || dataSitio.habitaciones().isEmpty()) {
            logger.debug("No hay habitaciones para evaluar; no se generan comandos");
            return new RespuestaCore(List.of());
        }
        logger.debug("Se evaluarán {} habitaciones", dataSitio.habitaciones().size());

        // Apaga todos los switches durante el horario punta
        if (dataSitio.sitio() != null
                && dataSitio.sitio().tarifa() != null
                && dataSitio.sitio().tarifa().esHorarioPunta(fechaHora)) {
            logger.debug("Horario punta activo; se apagarán los switches que estén encendidos");
            List<Comando> comandos = new ArrayList<>();
            for (Habitacion habitacion : dataSitio.habitaciones()) {
                agregarComandoSiNecesario(comandos, habitacion, EstadoSwitch.APAGADO);
            }
            logger.debug("Cálculo en horario punta finalizado: {} comandos", comandos.size());
            return new RespuestaCore(comandos);
        }

        // Lista de habitaciones que alcanzaron o están por encima de la temperatura esperada
        List<Habitacion> habitacionesSobreTemperaturaEsperada = dataSitio.habitaciones().stream()
            .filter(habitacion -> habitacion.currentTemperatureC() >= habitacion.expectedTemeprature())
            .toList();

        // Lista de habitaciones que están por debajo de la temperatura esperada,
        // ordenadas por la diferencia entre la temperatura esperada y la actual (de mayor a menor)
        List<Habitacion> habitacionesBajoTemperaturaEsperada = dataSitio.habitaciones().stream()
            .filter(habitacion -> habitacion.currentTemperatureC() < habitacion.expectedTemeprature())
            .sorted(Comparator.comparingDouble((Habitacion habitacion) ->
                    habitacion.expectedTemeprature() - habitacion.currentTemperatureC()).reversed())
            .toList();
        logger.debug("Habitaciones en o sobre temperatura esperada: {}; bajo temperatura esperada: {}",
                habitacionesSobreTemperaturaEsperada.size(), habitacionesBajoTemperaturaEsperada.size());

        // Calcula la potencia utilizada sumando la potencia de las habitaciones cuyo switch está encendido
        double potenciaUtilizada = dataSitio.habitaciones().stream()
            .filter(habitacion -> habitacion.currentSwitchState() == EstadoSwitch.ENCENDIDO)
            .mapToDouble(Habitacion::powerKW)
            .sum();
        logger.debug("Potencia utilizada inicialmente: {} kW", potenciaUtilizada);

        List<Comando> comandos = new ArrayList<>();

        /* - Agrega los comandos para apagar los switches de las habitaciones que están por encima de la temperatura esperada y está encendido
         * -Libera potencia para poder encender las habitaciones que están por debajo de la temperatura esperada
         */
        for (Habitacion habitacion : habitacionesSobreTemperaturaEsperada) {
            if (habitacion.currentSwitchState() != EstadoSwitch.APAGADO) {
                comandos.add(new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.APAGADO));
                potenciaUtilizada -= habitacion.powerKW();
                logger.debug("Se apaga el switch {}: temperatura actual {} °C, esperada {} °C; potencia liberada {} kW",
                        habitacion.idSwitch(), habitacion.currentTemperatureC(), habitacion.expectedTemeprature(),
                        habitacion.powerKW());
            }
        }

        /* - Agrega los comandos para encender los switches de las habitaciones que están por debajo de la temperatura esperada y cuyo switch está apagado.
         * - Solo enciende la habitación si no supera la potencia contratada.
         * - Si la habitación con mayor déficit de temperatura no puede encenderse por superar la potencia contratada,
         *     se intenta encender la siguiente habitación con menor déficit de temperatura, y así sucesivamente.
        */
        for (Habitacion habitacion : habitacionesBajoTemperaturaEsperada) {
            if (habitacion.currentSwitchState() != EstadoSwitch.ENCENDIDO) {
                assert dataSitio.sitio() != null;
                // Solo enciende la habitación si no supera la potencia contratada
                if (potenciaUtilizada + habitacion.powerKW() <= dataSitio.sitio().potenciaContratadaKW()) {
                    comandos.add(new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.ENCENDIDO));
                    potenciaUtilizada += habitacion.powerKW();
                    logger.debug("Se enciende el switch {}: déficit de temperatura {} °C; potencia utilizada {}/{} kW",
                            habitacion.idSwitch(),
                            habitacion.expectedTemeprature() - habitacion.currentTemperatureC(),
                            potenciaUtilizada, dataSitio.sitio().potenciaContratadaKW());
                } else {
                    logger.debug("No se enciende el switch {}: potencia requerida {} kW supera el límite contratado de {} kW",
                            habitacion.idSwitch(), potenciaUtilizada + habitacion.powerKW(),
                            dataSitio.sitio().potenciaContratadaKW());
                }
            }
        }

        logger.debug("Cálculo finalizado: {} comandos; potencia utilizada final: {} kW",
                comandos.size(), potenciaUtilizada);
        return new RespuestaCore(comandos);
    }

    private void agregarComandoSiNecesario(
            List<Comando> comandos,
            Habitacion habitacion,
            EstadoSwitch estadoDeseado
    ) {
        if (habitacion.currentSwitchState() != estadoDeseado) {
            comandos.add(new ComandoSwitch(habitacion.idSwitch(), estadoDeseado));
            logger.debug("Se agrega comando para el switch {}: estado {}", habitacion.idSwitch(), estadoDeseado);
        } else {
            logger.debug("El switch {} ya está en estado {}; no se genera comando",
                    habitacion.idSwitch(), estadoDeseado);
        }
    }

}
