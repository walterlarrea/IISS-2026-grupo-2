package com.iiss.sensores.modelos;


import java.util.List;

public record RespuestaCore(
        List<Comando> comandos
) {}
