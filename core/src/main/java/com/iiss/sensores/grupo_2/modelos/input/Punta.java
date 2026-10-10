package com.iiss.sensores.grupo_2.modelos.input;

import com.iiss.sensores.grupo_2.enums.DiasPunta;

public record Punta(
        String desde,
        String hasta,
        DiasPunta dias
) {}