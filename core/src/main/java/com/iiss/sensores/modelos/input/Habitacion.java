package com.iiss.sensores.modelos.input;

import com.iiss.sensores.enums.EstadoSwitch;

public record Habitacion(
        String id,
        String nombre,
        double temperaturaEsperada,
        double potenciaKW,
        String idTermostato,
        String topicTermostato,
        String idSwitch,
        String urlSwitch,

        double temperaturaActual,
        EstadoSwitch estadoActual
) {}