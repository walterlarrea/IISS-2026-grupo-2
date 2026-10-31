package com.iiss.sensores.grupo_2.mcpserver.client.dto;

import java.time.Instant;

/**
 * Local DTO mirroring the switches-api SwitchResponse.
 * Reflects GET /switches/{id} and POST /switches/{id}/comandos responses.
 */
public record SwitchResponseDto(
        String idSwitch,
        String estado,
        Instant aplicadoEn
) {}
