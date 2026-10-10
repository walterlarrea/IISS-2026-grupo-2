package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;

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