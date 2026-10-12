package com.iiss.sensores.grupo_2.modelos.input;

import java.time.ZonedDateTime;

public record Tarifa(Punta punta) {
    public boolean esHorarioPunta(ZonedDateTime fechaHora) {
        return punta != null && punta.esHorarioPunta(fechaHora);
    }
}