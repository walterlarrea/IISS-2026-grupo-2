package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.output.Comando;
import com.iiss.sensores.grupo_2.modelos.output.ComandoSwitch;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Core implements ICore {
    public Core() {
        // Constructor de la clase Core
    }

    @Override
    public RespuestaCore calcularComandos(ZonedDateTime fechaHora, DataSitio dataSitio) {
        if (dataSitio == null || dataSitio.habitaciones() == null || dataSitio.habitaciones().isEmpty()) {
            // Podría lanzar una excepción?
            return new RespuestaCore(List.of());
        }

        // Apaga todos los switches durante el horario punta
        if (dataSitio.sitio() != null
                && dataSitio.sitio().tarifa() != null
                && dataSitio.sitio().tarifa().esHorarioPunta(fechaHora)) {
            List<Comando> comandos = new ArrayList<>();
            for (Habitacion habitacion : dataSitio.habitaciones()) {
                agregarComandoSiNecesario(comandos, habitacion, EstadoSwitch.APAGADO);
            }
            return new RespuestaCore(comandos);
        }

        // Lista de habitaciones que alcanzaron o están por encima de la temperatura esperada
        List<Habitacion> habitacionesSobreTemperaturaEsperada = dataSitio.habitaciones().stream()
            .filter(habitacion -> habitacion.temperaturaActual() >= habitacion.temperaturaEsperada())
            .toList();

        // Lista de habitaciones que están por debajo de la temperatura esperada,
        // ordenadas por la diferencia entre la temperatura esperada y la actual (de mayor a menor)
        List<Habitacion> habitacionesBajoTemperaturaEsperada = dataSitio.habitaciones().stream()
            .filter(habitacion -> habitacion.temperaturaActual() < habitacion.temperaturaEsperada())
            .sorted(Comparator.comparingDouble((Habitacion habitacion) ->
                    habitacion.temperaturaEsperada() - habitacion.temperaturaActual()).reversed())
            .toList();

        // Calcula la potencia utilizada sumando la potencia de las habitaciones cuyo switch está encendido
        double potenciaUtilizada = dataSitio.habitaciones().stream()
            .filter(habitacion -> habitacion.estadoActualSwitch() == EstadoSwitch.ENCENDIDO)
            .mapToDouble(Habitacion::potenciaKW)
            .sum();

        List<Comando> comandos = new ArrayList<>();

        /* - Agrega los comandos para apagar los switches de las habitaciones que están por encima de la temperatura esperada y está encendido
         * -Libera potencia para poder encender las habitaciones que están por debajo de la temperatura esperada
         */
        for (Habitacion habitacion : habitacionesSobreTemperaturaEsperada) {
            if (habitacion.estadoActualSwitch() != EstadoSwitch.APAGADO) {
                comandos.add(new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.APAGADO));
                potenciaUtilizada -= habitacion.potenciaKW();
            }
        }

        /* - Agrega los comandos para encender los switches de las habitaciones que están por debajo de la temperatura esperada y cuyo switch está apagado.
         * - Solo enciende la habitación si no supera la potencia contratada.
         * - Si la habitación con mayor déficit de temperatura no puede encenderse por superar la potencia contratada,
         *     se intenta encender la siguiente habitación con menor déficit de temperatura, y así sucesivamente.
        */
        for (Habitacion habitacion : habitacionesBajoTemperaturaEsperada) {
            if (habitacion.estadoActualSwitch() != EstadoSwitch.ENCENDIDO) {
                // Solo enciende la habitación si no supera la potencia contratada
                if (potenciaUtilizada + habitacion.potenciaKW() <= dataSitio.sitio().potenciaContratadaKW()) {
                    comandos.add(new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.ENCENDIDO));
                    potenciaUtilizada += habitacion.potenciaKW();
                }
            }
        }
        
        return new RespuestaCore(comandos);
    }

    private void agregarComandoSiNecesario(
            List<Comando> comandos,
            Habitacion habitacion,
            EstadoSwitch estadoDeseado
    ) {
        if (habitacion.estadoActualSwitch() != estadoDeseado) {
            comandos.add(new ComandoSwitch(habitacion.idSwitch(), estadoDeseado));
        }
    }

}
