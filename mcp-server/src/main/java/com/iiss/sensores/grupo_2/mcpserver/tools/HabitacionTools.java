package com.iiss.sensores.grupo_2.mcpserver.tools;

import com.iiss.sensores.grupo_2.mcpserver.client.ControllerClient;
import com.iiss.sensores.grupo_2.mcpserver.client.dto.HabitacionDto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP tools for room configuration: querying and updating the definition of rooms
 * (thermostat, switch, expected temperature).
 *
 * <p>Every method is a thin delegator to {@link ControllerClient}. No business logic lives here.
 */
@Service
public class HabitacionTools {

    private final ControllerClient controllerClient;

    public HabitacionTools(ControllerClient controllerClient) {
        this.controllerClient = controllerClient;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Tool(name = "listar_habitaciones",
            description = """
                    Returns the complete list of rooms (habitaciones) configured in the site inventory.
                    Each room includes: its unique ID, name, expected temperature (Celsius), power in kW,
                    thermostat ID, thermostat MQTT topic, switch ID, and switch base URL.
                    Use this tool when the user wants to see the current room configuration, when they
                    ask "what rooms are there?", "show me the rooms", or before modifying a room to
                    discover its current values.
                    """)
    public List<HabitacionDto> listarHabitaciones() {
        return controllerClient.listarHabitaciones();
    }

    @Tool(name = "obtener_habitacion",
            description = """
                    Returns the full configuration of a single room identified by its ID.
                    Use this tool when the user refers to a specific room by name or ID, for example
                    "show me room living", "what is the configuration of room1?", or when you need
                    to look up the switch URL or thermostat ID of a particular room before acting on it.
                    """)
    public HabitacionDto obtenerHabitacion(
            @ToolParam(description = "The unique room ID, e.g. 'room1' or 'hab-01'. "
                    + "Use listar_habitaciones first if you don't know the exact ID.") String id) {
        return controllerClient.obtenerHabitacion(id);
    }

    // ── Modificaciones ────────────────────────────────────────────────────────

    @Tool(name = "modificar_temperatura_esperada",
            description = """
                    Updates the target (expected) temperature for a specific room.
                    The temperature is in degrees Celsius and can include one decimal place.
                    Use this tool when the user says things like:
                    "set room X to 22 degrees", "I want the living room cooler, set it to 19",
                    "raise the temperature of the bedroom to 23.5", or any similar instruction
                    to change a room's setpoint. The change is persisted immediately.
                    """)
    public HabitacionDto modificarTemperaturaEsperada(
            @ToolParam(description = "The unique room ID to update. "
                    + "Use listar_habitaciones if you need to find the correct ID.") String id,
            @ToolParam(description = "New target temperature in degrees Celsius (e.g. 21.5). "
                    + "Must be a positive number.") double temperaturaEsperada) {
        return controllerClient.modificarHabitacionParcialmente(id,
                java.util.Map.of("temperaturaEsperada", temperaturaEsperada));
    }

    @Tool(name = "crear_habitacion",
            description = """
                    Adds a new room to the site inventory with all required fields.
                    Use this tool when the user wants to add a new room to the system, for example
                    "add a new room called Garage with thermostat ht-garage and switch sw-garage".
                    All fields are mandatory:
                    - id: unique identifier for the room (e.g. "room3")
                    - nombre: human-readable name (max 50 characters, e.g. "Garage")
                    - temperaturaEsperada: target temperature in Celsius (e.g. 18.0)
                    - potenciaKW: power of the climate device in kW (e.g. 1.5)
                    - idTermostato: thermostat device ID (unique in the site)
                    - topicTermostato: MQTT topic where the thermostat publishes (e.g. "ht-garage/status/temperature:0")
                    - idSwitch: switch device ID (unique in the site)
                    - urlSwitch: base HTTP URL of the switch API (e.g. "http://switches:8090")
                    If a room with the same ID already exists, the existing room is returned unchanged.
                    """)
    public HabitacionDto crearHabitacion(
            @ToolParam(description = "Unique room ID (e.g. 'room3')") String id,
            @ToolParam(description = "Human-readable room name, max 50 chars (e.g. 'Garage')") String nombre,
            @ToolParam(description = "Target temperature in Celsius (e.g. 18.0)") double temperaturaEsperada,
            @ToolParam(description = "Climate device power in kW, must be > 0 (e.g. 1.5)") double potenciaKW,
            @ToolParam(description = "Thermostat device ID, unique in the site (e.g. 'ht-garage')") String idTermostato,
            @ToolParam(description = "MQTT topic where the thermostat publishes (e.g. 'ht-garage/status/temperature:0')") String topicTermostato,
            @ToolParam(description = "Switch device ID, unique in the site (e.g. 'sw-garage')") String idSwitch,
            @ToolParam(description = "Base URL of the switch API (e.g. 'http://switches:8090')") String urlSwitch) {
        return controllerClient.crearHabitacion(
                new HabitacionDto(id, nombre, temperaturaEsperada, potenciaKW,
                        idTermostato, topicTermostato, idSwitch, urlSwitch));
    }
}
