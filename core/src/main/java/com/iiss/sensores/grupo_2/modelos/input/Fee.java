package com.iiss.sensores.grupo_2.modelos.input;

import java.time.ZonedDateTime;

public record Fee(RushHour rushHour) {
    public boolean esHorarioPunta(ZonedDateTime fechaHora) {
        return rushHour != null && rushHour.esHorarioPunta(fechaHora);
    }
}