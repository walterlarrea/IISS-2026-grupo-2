package com.iiss.sensores.grupo_2.modelos.input;

public record Sitio(
        String id,
        String nombre,
        double potenciaContratadaKW,
        Tarifa tarifa
) {}