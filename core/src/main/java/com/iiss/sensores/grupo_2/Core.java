package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.enums.DiasPunta;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.input.Punta;
import com.iiss.sensores.grupo_2.modelos.output.Comando;
import com.iiss.sensores.grupo_2.modelos.output.ComandoSwitch;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Core implements ICore {
    public Core() {
        // Constructor de la clase Core
    }

    // Faltan los checks del estado actual de un switch antes de enviar un comando para encenderlo o apagarlo
    @Override
    public RespuestaCore calcularComandos(ZonedDateTime fechaHora, DataSitio dataSitio) {
        if (dataSitio == null || dataSitio.habitaciones() == null || dataSitio.habitaciones().isEmpty()) {
            // Podría lanzar una excepción?
            return new RespuestaCore(List.of());
        }

        // Apagar todos los switches durante el horario punta
        if (esHorarioPunta(fechaHora, dataSitio)) {
            List<Comando> comandos = new ArrayList<>();
            for (Habitacion habitacion : dataSitio.habitaciones()) {
                comandos.add((Comando) new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.APAGADO));
            }
            return new RespuestaCore(comandos);
        }

        List<Habitacion> habitacionesBajoTemperatura = dataSitio.habitaciones().stream()
                .filter(habitacion -> habitacion.temperaturaActual() < habitacion.temperaturaEsperada())
                .toList();

        Habitacion habitacionSeleccionada = null;
        if (!habitacionesBajoTemperatura.isEmpty()) {
            // Acá debería armar una lista en vez de solo tomar una única habitación de mayor déficit
            habitacionSeleccionada = habitacionesBajoTemperatura.stream()
                    .max(Comparator.comparingDouble(habitacion -> habitacion.temperaturaEsperada() - habitacion.temperaturaActual()))
                    .orElse(null);

            if (habitacionSeleccionada != null) {
                double potenciaActual = dataSitio.habitaciones().stream()
                        .filter(habitacion -> habitacion.estadoActualSwitch() == EstadoSwitch.ENCENDIDO)
                        .mapToDouble(Habitacion::potenciaKW)
                        .sum();

                double potenciaLibre = dataSitio.sitio().potenciaContratadaKW() - potenciaActual;
                if (potenciaLibre < habitacionSeleccionada.potenciaKW()) {
                    habitacionSeleccionada = null;
                }
            }
        }

        List<Comando> comandos = new ArrayList<>();
        for (Habitacion habitacion : dataSitio.habitaciones()) {
            // Tiene que apagar antes de prender otras, así se libera
            if (habitacion.temperaturaActual() > habitacion.temperaturaEsperada()) {
                comandos.add((Comando) new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.APAGADO));
            } else if (habitacionSeleccionada != null && habitacion.id().equals(habitacionSeleccionada.id())) {
                comandos.add((Comando) new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.ENCENDIDO));
            } else if (habitacion.temperaturaActual() < habitacion.temperaturaEsperada()) {
                comandos.add((Comando) new ComandoSwitch(habitacion.idSwitch(), EstadoSwitch.APAGADO));
            }
        }

        return new RespuestaCore(comandos);
    }

    private boolean esHorarioPunta(ZonedDateTime fechaHora, DataSitio dataSitio) {
        if (dataSitio == null || dataSitio.sitio() == null || dataSitio.sitio().tarifa() == null) {
            return false;
        }

        Punta punta = dataSitio.sitio().tarifa().punta();
        if (punta == null || punta.dias() == null) {
            return false;
        }

        if (punta.dias() != DiasPunta.HABILES) {
            return false;
        }

        // Debería usar .toLocalTime() para obtener la hora local antes de .getDayOfWeek()?
        DayOfWeek diaSemana = fechaHora.getDayOfWeek();
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime horaActual = fechaHora.toLocalTime();
        LocalTime horaInicio = LocalTime.parse(punta.desde());
        LocalTime horaFin = LocalTime.parse(punta.hasta());

        if (horaInicio.isBefore(horaFin)) {
            return !horaActual.isBefore(horaInicio) && !horaActual.isAfter(horaFin);
        }

        return !horaActual.isBefore(horaInicio) || !horaActual.isAfter(horaFin);
    }
}
