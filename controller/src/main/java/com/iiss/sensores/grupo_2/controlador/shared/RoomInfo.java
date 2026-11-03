package com.iiss.sensores.grupo_2.controlador.shared;

/** Datos compartidos de configuración de una habitación y sus dispositivos. */
public record RoomInfo(
    String id,
    String nombre,
    Double temperaturaEsperada,
    Double potenciaKW,
    String idTermostato,
    String topicTermostato,
    String idSwitch,
    String urlSwitch) {}
