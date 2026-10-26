package com.iiss.sensores.grupo_2.modelos.input;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.iiss.sensores.grupo_2.enums.RushDays;

public record RushHour(
        String desde,
        String hasta,
        RushDays dias
) {
    public boolean esHorarioPunta(ZonedDateTime fechaHora) {
        if (fechaHora == null || this.dias == null) {
            return false;
        }

        switch (this.dias) {
            case RushDays.HABILES:
                DayOfWeek diaSemana = fechaHora.getDayOfWeek();
                if (
                    this.dias == RushDays.HABILES &&
                    (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY)
                ) {
                    return false;
                }
                break;
            case RushDays.TODOS:
                // No se requiere ninguna verificación adicional para todos los días
                break;
            default:
                break;
        }

        LocalTime horaActual = fechaHora.toLocalTime();
        LocalTime horaInicio = LocalTime.parse(this.desde);
        LocalTime horaFin = LocalTime.parse(this.hasta);

        return !horaActual.isBefore(horaInicio) && !horaActual.isAfter(horaFin);
    }
}