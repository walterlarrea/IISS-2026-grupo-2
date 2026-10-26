package com.iiss.sensores.grupo_2.controlador.api.model;

import java.time.Instant;

public record StateControl(
        Estado estado,
        Instant fechaHora,
        Integer factorTiempo,
        Instant iniciadoEn
) {
    public enum Estado {
        EN_EJECUCION,
        DETENIDO
    }
}
