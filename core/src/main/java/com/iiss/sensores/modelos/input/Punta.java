package com.iiss.sensores.modelos.input;

import com.iiss.sensores.enums.DiasPunta;

public record Punta(
        String desde,
        String hasta,
        DiasPunta dias
) {}