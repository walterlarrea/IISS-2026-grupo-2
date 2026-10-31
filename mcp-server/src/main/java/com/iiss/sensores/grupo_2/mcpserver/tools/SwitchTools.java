package com.iiss.sensores.grupo_2.mcpserver.tools;

import com.iiss.sensores.grupo_2.mcpserver.client.ControllerClient;
import com.iiss.sensores.grupo_2.mcpserver.client.dto.SwitchResponseDto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tools for switch actuation: turning room climate devices ON or OFF.
 *
 * <p>The MCP server does not contact the switches-api directly: the switch's URL and device ID
 * are resolved by {@link ControllerClient} via the room configuration, keeping all topology
 * knowledge in one place.
 */
@Service
public class SwitchTools {

    private final ControllerClient controllerClient;

    public SwitchTools(ControllerClient controllerClient) {
        this.controllerClient = controllerClient;
    }

    @Tool(name = "activar_switch",
            description = """
                    Turns ON the electrical switch of a specific room, activating the climate
                    control device (heating or air conditioning) in that room.
                    Use this tool when the user says things like:
                    "turn on room X", "start heating the living room", "activate the device in room Y",
                    "switch on the air conditioning in the bedroom", or similar instructions to
                    manually start a room's climate device.
                    The operation is forwarded to the switch via the controller's room configuration.
                    """)
    public SwitchResponseDto activarSwitch(
            @ToolParam(description = "The unique room ID whose switch should be turned ON. "
                    + "Use listar_habitaciones if you need to look up the correct ID.") String idHabitacion) {
        return controllerClient.accionarSwitch(idHabitacion, "ON");
    }

    @Tool(name = "desactivar_switch",
            description = """
                    Turns OFF the electrical switch of a specific room, deactivating the climate
                    control device in that room.
                    Use this tool when the user says things like:
                    "turn off room X", "stop heating the living room", "deactivate the device in room Y",
                    "switch off the air conditioning in the bedroom", or similar instructions to
                    manually stop a room's climate device.
                    The operation is forwarded to the switch via the controller's room configuration.
                    """)
    public SwitchResponseDto desactivarSwitch(
            @ToolParam(description = "The unique room ID whose switch should be turned OFF. "
                    + "Use listar_habitaciones if you need to look up the correct ID.") String idHabitacion) {
        return controllerClient.accionarSwitch(idHabitacion, "OFF");
    }

    @Tool(name = "consultar_estado_switch",
            description = """
                    Returns the current ON/OFF state of the switch associated with a specific room,
                    along with the timestamp of the last state change.
                    Use this tool when the user asks "is the switch in room X on or off?",
                    "what is the switch state of the bedroom?", or before deciding whether to
                    activate or deactivate a switch.
                    """)
    public SwitchResponseDto consultarEstadoSwitch(
            @ToolParam(description = "The unique room ID whose switch state should be queried. "
                    + "Use listar_habitaciones if you need to look up the correct ID.") String idHabitacion) {
        return controllerClient.estadoSwitch(idHabitacion);
    }
}
