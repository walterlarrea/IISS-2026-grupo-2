package com.iiss.sensores.grupo_2.modelos.input;

/** Representa la configuración general y el límite de potencia de un sitio. */
public record Site(String id, String nombre, double potenciaContratadaKW, Fee fee) {}
