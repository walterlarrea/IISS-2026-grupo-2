package com.iiss.sensores.grupo_2.dto;

import java.time.Instant;

/** Representa una medición de temperatura recibida de un termostato. */
public record TemperatureMessage(
    String id, String idThermostat, double tC, double tF, Instant ts) {}
