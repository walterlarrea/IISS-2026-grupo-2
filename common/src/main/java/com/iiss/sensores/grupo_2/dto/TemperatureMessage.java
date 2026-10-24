package com.iiss.sensores.grupo_2.dto;


import java.time.Instant;

public record TemperatureMessage(
        String id,
        String idTermostat,
        double tC,
        double tF,
        Instant ts
) {
}
