package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.input.*;
import com.iiss.sensores.grupo_2.modelos.output.CommandSwitch;
import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwitchClientRestFailureIT {
    @Test
    void reportsConnectionFailureWhenRemoteSwitchApiIsOffline() throws Exception {
        // Comprueba que el cliente REST informa el fallo cuando la API remota no está disponible.
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        int port = server.getAddress().getPort();
        server.stop(0);

        SwitchClient client = new SwitchClient(RestClient.builder().build(), "integration-key");
        Room habitacion = new Room(
                "room-1",
                "Room 1",
                21.0,
                1.0,
                "thermostat-1",
                "integration/room-1",
                "switch-1",
                "http://127.0.0.1:" + port,
                20.0,
                SwitchState.APAGADO);

        assertThrows(ResourceAccessException.class, () -> client.encender(habitacion));
    }

    @Test
    void reportsServiceUnavailableWhenRemoteSwitchApiFails() throws Exception {
        // Comprueba que el cliente propaga HTTP 503 cuando la API de switches falla.
        AtomicInteger receivedRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/switches/switch-1/comandos", exchange -> {
            receivedRequests.incrementAndGet();
            byte[] response = """
                    {"error":"switch service unavailable"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.start();

        try {
            SwitchClient client = new SwitchClient(RestClient.builder().build(), "integration-key");
            Room habitacion = new Room(
                    "room-1",
                    "Room 1",
                    21.0,
                    1.0,
                    "thermostat-1",
                    "integration/room-1",
                    "switch-1",
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    20.0,
                    SwitchState.APAGADO);

            HttpServerErrorException.ServiceUnavailable error = assertThrows(
                    HttpServerErrorException.ServiceUnavailable.class,
                    () -> client.encender(habitacion));

            assertEquals(503, error.getStatusCode().value());
            assertEquals(1, receivedRequests.get(), "The REST request should reach the switch API");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void keepsLastKnownSwitchStateWhenControllerCommandCannotReachSwitch() throws Exception {
        // Comprueba que el controlador registra el fallo del switch y conserva el último estado conocido.
        AtomicInteger commandRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/switches/switch-1", exchange -> {
            byte[] response = """
                    {"idSwitch":"switch-1","estado":"OFF","aplicadoEn":"2026-10-25T00:00:00Z"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.createContext("/switches/switch-1/comandos", exchange -> {
            commandRequests.incrementAndGet();
            byte[] response = """
                    {"error":"switch service unavailable"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(503, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.start();

        try {
            String switchUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            SwitchClient switchClient = new SwitchClient(RestClient.builder().build(), "integration-key");
            TemperatureReadingRepository readings = mock(TemperatureReadingRepository.class);
            ICore core = mock(ICore.class);
            SiteStateService siteState = new SiteStateService(switchClient, readings);
            Room room = new Room(
                    "room-1",
                    "Room 1",
                    21.0,
                    1.0,
                    "thermostat-1",
                    "integration/room-1",
                    "switch-1",
                    switchUrl,
                    null,
                    SwitchState.APAGADO);
            siteState.loadSiteState(new DataSite(
                    new Site("site-1", "Site", 5.0, new Fee(null)),
                    List.of(room)));

            when(readings.getLastTemperatureMeasurements(any())).thenReturn(List.of(
                    new TemperatureMessage(
                            "reading-1",
                            "thermostat-1",
                            22.0,
                            71.6,
                            Instant.parse("2026-10-25T10:00:00Z"))));
      when(core.calcularComandos(any(), any()))
          .thenReturn(
              new CoreAnswer(List.of(new CommandSwitch("switch-1", SwitchState.ENCENDIDO))));
            ControllerTempAuto controller = new ControllerTempAuto(switchClient, core, siteState);
            controller.start(Instant.parse("2026-10-25T10:00:00Z"), 1);

            controller.processTemperature(new TemperatureMessage(
                    "reading-2",
                    "thermostat-1",
                    22.0,
                    71.6,
                    Instant.parse("2026-10-25T10:01:00Z")));

            assertEquals(1, commandRequests.get(), "The controller should attempt the switch command");
            assertEquals(
                    SwitchState.APAGADO,
                    siteState.getSiteState().habitaciones().get(0).currentSwitchState(),
                    "A failed remote command must not update the locally known switch state");
        } finally {
            server.stop(0);
        }
    }
}
