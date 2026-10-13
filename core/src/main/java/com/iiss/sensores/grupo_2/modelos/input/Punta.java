package com.iiss.sensores.grupo_2.modelos.input;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.iiss.sensores.grupo_2.enums.DiasPunta;

public record Punta(
        String desde,
        String hasta,
        DiasPunta dias
) {
    public boolean esHorarioPunta(ZonedDateTime fechaHora) {
        if (fechaHora == null || this.dias == null) {
            return false;
        }

        DayOfWeek diaSemana = fechaHora.getDayOfWeek();
        if (
            this.dias == DiasPunta.HABILES &&
            (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY)
        ) {
            return false;
        }

        LocalTime horaActual = fechaHora.toLocalTime();
        LocalTime horaInicio = LocalTime.parse(this.desde);
        LocalTime horaFin = LocalTime.parse(this.hasta);

        return !horaActual.isBefore(horaInicio) && !horaActual.isAfter(horaFin);
    }
}