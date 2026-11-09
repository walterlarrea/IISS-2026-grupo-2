package com.iiss.sensores.grupo_2.mcpserver.client;

import com.iiss.sensores.grupo_2.mcpserver.client.dto.EstadoControlDto;
import com.iiss.sensores.grupo_2.mcpserver.client.dto.HabitacionDto;
import com.iiss.sensores.grupo_2.mcpserver.client.dto.SwitchResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin HTTP adapter that wraps all calls to the existing controller REST API and to
 * the switches-api.
 *
 * <p>This is the <em>only</em> class in the MCP server module that performs I/O.
 * The tool classes delegate entirely to it, keeping each tool as a one-liner.
 *
 * <p>Endpoints consumed from the controller (see controller-api-openapi.yml):
 * <ul>
 *   <li>{@code GET  /habitaciones}
 *   <li>{@code GET  /habitaciones/{id}}
 *   <li>{@code POST /habitaciones}
 *   <li>{@code PATCH /habitaciones/{id}}
 *   <li>{@code GET  /sitio}
 *   <li>{@code GET  /control}
 *   <li>{@code POST /control/comandos}
 * </ul>
 *
 * <p>Endpoints consumed from the switches-api:
 * <ul>
 *   <li>{@code GET  /switches/{idSwitch}}           — via the switch's base URL
 *   <li>{@code POST /switches/{idSwitch}/comandos}  — via the switch's base URL
 * </ul>
 */
@Component
public class ControllerClient {

    private final RestClient controllerRestClient;
    private final String switchesApiKey;

    public ControllerClient(
            @Value("${controller.base-url}") String controllerBaseUrl,
            @Value("${controller.api-key}") String controllerApiKey,
            @Value("${switches.api-key}") String switchesApiKey) {

        this.controllerRestClient = RestClient.builder()
                .baseUrl(controllerBaseUrl)
                .defaultHeader("X-API-KEY", controllerApiKey)
                .build();
        this.switchesApiKey = switchesApiKey;
    }

    // ─────────────────────────────────────────────
    // Habitaciones
    // ─────────────────────────────────────────────

    /** Returns all rooms defined in the active inventory. */
    public List<HabitacionDto> listarHabitaciones() {
        return controllerRestClient.get()
                .uri("/habitaciones")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /** Returns a single room by its ID, or null if not found. */
    public HabitacionDto obtenerHabitacion(String id) {
        return controllerRestClient.get()
                .uri("/habitaciones/{id}", id)
                .retrieve()
                .body(HabitacionDto.class);
    }

    /** Creates a new room in the active inventory. */
    public HabitacionDto crearHabitacion(HabitacionDto habitacion) {
        return controllerRestClient.post()
                .uri("/habitaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .body(habitacion)
                .retrieve()
                .body(HabitacionDto.class);
    }

    /**
     * Partially updates a room.
     *
     * @param id     room ID
     * @param cambios fields to update (e.g. {@code {"temperaturaEsperada": 22.0}})
     */
    public HabitacionDto modificarHabitacionParcialmente(String id, Map<String, Object> cambios) {
        return controllerRestClient.patch()
                .uri("/habitaciones/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cambios)
                .retrieve()
                .body(HabitacionDto.class);
    }

    // ─────────────────────────────────────────────
    // Estado del controlador
    // ─────────────────────────────────────────────

    /** Returns the current status of the Temperature Controller engine. */
    public EstadoControlDto obtenerEstadoControl() {
        return controllerRestClient.get()
                .uri("/control")
                .retrieve()
                .body(EstadoControlDto.class);
    }

    /**
     * Sends a command to the engine.
     *
     * @param body the command payload, e.g.
     *             {@code {"accion":"INICIAR","fechaHora":"...","factorTiempo":1}}
     *             or {@code {"accion":"DETENER"}}
     */
    public EstadoControlDto comandarControl(Map<String, Object> body) {
        return controllerRestClient.post()
                .uri("/control/comandos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(EstadoControlDto.class);
    }

    // ─────────────────────────────────────────────
    // Switches (via switches-api base URL stored per room)
    // ─────────────────────────────────────────────

    /**
     * Activates or deactivates the switch for the given room.
     *
     * <p>The switch base URL ({@code urlSwitch}) and switch ID ({@code idSwitch}) are fetched
     * first from the controller, then the command is forwarded to the switches-api.
     *
     * @param idHabitacion room whose switch should change state
     * @param estado        "ON" or "OFF"
     * @return the switches-api response
     */
    public SwitchResponseDto accionarSwitch(String idHabitacion, String estado) {
        // 1. Resolve the room to obtain its switch coordinates
        HabitacionDto room = obtenerHabitacion(idHabitacion);

        // 2. Build a one-off RestClient targeting the switch's own base URL
        RestClient switchClient = RestClient.builder()
                .baseUrl(room.urlSwitch())
                .defaultHeader("X-API-KEY", switchesApiKey)
                .build();

        // 3. POST /switches/{idSwitch}/comandos  {"accion":"ON|OFF"}
        return switchClient.post()
                .uri("/switches/{idSwitch}/comandos", room.idSwitch())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("accion", estado))
                .retrieve()
                .body(SwitchResponseDto.class);
    }

    /**
     * Returns the current state of the switch associated with a room.
     *
     * @param idHabitacion room to query
     */
    public SwitchResponseDto estadoSwitch(String idHabitacion) {
        HabitacionDto room = obtenerHabitacion(idHabitacion);

        RestClient switchClient = RestClient.builder()
                .baseUrl(room.urlSwitch())
                .defaultHeader("X-API-KEY", switchesApiKey)
                .build();

        return switchClient.get()
                .uri("/switches/{idSwitch}", room.idSwitch())
                .retrieve()
                .body(SwitchResponseDto.class);
    }
}
