package com.iiss.sensores.grupo_2.controlador.shared;

public record RoomInfo(
        String id,
        String nombre,
        double temperaturaEsperada,
        String idTermostato,
        String uriSwitch
) {
}
