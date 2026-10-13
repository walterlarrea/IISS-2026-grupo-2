package com.iiss.sensores.grupo_2.modelos.output;

import java.util.List;

public record RespuestaCore(
        List<Comando> comandos
) {}
