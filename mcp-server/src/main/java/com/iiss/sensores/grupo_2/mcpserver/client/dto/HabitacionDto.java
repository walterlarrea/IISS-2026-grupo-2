package com.iiss.sensores.grupo_2.mcpserver.client.dto;

/**
 * Local DTO mirroring the controller's Inventory.Habitacion record.
 * The MCP server does NOT import the controller module; it uses its own
 * representations and communicates exclusively over HTTP.
 */
public record HabitacionDto(
        String id,
        String nombre,
        Double temperaturaEsperada,
        Double potenciaKW,
        String idTermostato,
        String topicTermostato,
        String idSwitch,
        String urlSwitch
) {}
