package com.iiss.sensores.modelos.input;

public record Sitio(
        String id,
        String nombre,
        double potenciaContratadaKW,
        Tarifa tarifa
) {}