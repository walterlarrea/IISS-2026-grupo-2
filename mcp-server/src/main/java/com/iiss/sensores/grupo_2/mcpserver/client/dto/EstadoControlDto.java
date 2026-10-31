package com.iiss.sensores.grupo_2.mcpserver.client.dto;

import java.time.Instant;

/**
 * Local DTO mirroring the controller's StateControl record.
 * Reflects GET /control and POST /control/comandos responses.
 */
public record EstadoControlDto(
        String estado,
        Instant fechaHora,
        Integer factorTiempo,
        Instant iniciadoEn
) {}
