package com.iiss.sensores.grupo_2.controlador.api.model;

import java.time.Instant;

public record EstadoControl(
        Estado estado,
        Instant fechaHora,
        Double factorTiempo,
        Instant iniciadoEn
) {
    public enum Estado {
        EN_EJECUCION,
        DETENIDO
    }
}
