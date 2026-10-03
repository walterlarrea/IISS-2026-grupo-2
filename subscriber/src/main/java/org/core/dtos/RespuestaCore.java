package org.core.dtos;

import java.util.List;

public record RespuestaCore(
        List<Comando> comandos
) {}
