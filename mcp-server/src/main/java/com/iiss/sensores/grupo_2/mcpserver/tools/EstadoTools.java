package com.iiss.sensores.grupo_2.mcpserver.tools;

import com.iiss.sensores.grupo_2.mcpserver.client.ControllerClient;
import com.iiss.sensores.grupo_2.mcpserver.client.dto.EstadoControlDto;
import com.iiss.sensores.grupo_2.mcpserver.client.dto.HabitacionDto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP tools for querying and commanding the system state:
 * controller engine status, per-room temperature and switch states,
 * and starting/stopping the engine.
 */
@Service
public class EstadoTools {

    private final ControllerClient controllerClient;

    public EstadoTools(ControllerClient controllerClient) {
        this.controllerClient = controllerClient;
    }

    // ── Consultas de estado ───────────────────────────────────────────────────

    @Tool(name = "obtener_estado_controlador",
            description = """
                    Returns the current status of the Temperature Controller engine.
                    The response includes:
                    - estado: "EN_EJECUCION" (running) or "DETENIDO" (stopped)
                    - fechaHora: the simulated logical time at which the controller is currently
                      evaluating (null when stopped)
                    - factorTiempo: time acceleration factor active (null when stopped, 1 = real time)
                    - iniciadoEn: real-world timestamp of the last INICIAR command (null when stopped)
                    Use this tool when the user asks "is the controller running?", "what is the system
                    status?", "is the engine active?", or before deciding whether to start or stop it.
                    """)
    public EstadoControlDto obtenerEstadoControlador() {
        return controllerClient.obtenerEstadoControl();
    }

    @Tool(name = "obtener_estado_habitaciones",
            description = """
                    Returns the current temperature and switch state of all configured rooms.
                    For each room the response includes the room name, its current measured
                    temperature (from the thermostat, in Celsius), its expected target temperature,
                    and the current ON/OFF state of its switch.
                    Use this tool when the user asks "what is the temperature in each room?",
                    "which rooms have their heating on?", "give me a dashboard of all rooms",
                    or any request for a real-time snapshot of the site state.
                    Note: rooms with no temperature reading yet are excluded from the result.
                    """)
    public List<HabitacionDto> obtenerEstadoHabitaciones() {
        return controllerClient.listarHabitaciones();
    }

    // ── Comandos al motor ─────────────────────────────────────────────────────

    @Tool(name = "iniciar_controlador",
            description = """
                    Starts the Temperature Controller engine from a given reference date/time.
                    After INICIAR, the controller uses a synthetic clock:
                        logical_time = fechaHora + (elapsed_real_time × factorTiempo)
                    This means the controller evaluates tariff windows and makes switch decisions
                    based on the synthetic clock, not the real system clock.
                    INICIAR on an already running engine re-synchronizes it (not an error).
                    Use this tool when the user says "start the controller", "begin simulation from
                    [date/time]", "run the system", or "start controlling from midnight".
                    The factorTiempo parameter is optional (defaults to 1 = real time); values > 1
                    accelerate the simulation (e.g. 10 = ten times faster than real time).
                    """)
    public EstadoControlDto iniciarControlador(
            @ToolParam(description = "ISO-8601 reference datetime from which the controller's "
                    + "logical clock starts, e.g. '2026-10-01T09:00:00Z'. Required.") String fechaHora,
            @ToolParam(description = "Time acceleration factor: positive integer. "
                    + "1 = real time (default), 10 = 10× faster. Optional, defaults to 1.") Integer factorTiempo) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("accion", "INICIAR");
        body.put("fechaHora", fechaHora);
        body.put("factorTiempo", factorTiempo != null ? factorTiempo : 1);
        return controllerClient.comandarControl(body);
    }

    @Tool(name = "detener_controlador",
            description = """
                    Stops the Temperature Controller engine.
                    After DETENER, the controller no longer evaluates temperatures or commands switches.
                    The engine does not automatically turn off any switch that was left ON.
                    DETENER on an already stopped engine is a no-op (idempotent).
                    Use this tool when the user says "stop the controller", "pause the system",
                    "shut it down", "stop controlling", or "turn off the engine".
                    """)
    public EstadoControlDto detenerControlador() {
        return controllerClient.comandarControl(Map.of("accion", "DETENER"));
    }
}
