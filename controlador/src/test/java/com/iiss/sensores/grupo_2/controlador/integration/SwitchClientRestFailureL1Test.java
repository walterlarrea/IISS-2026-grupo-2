package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("Layer1")
class SwitchClientRestFailureL1Test {
    @Test
    void reportsConnectionFailureWhenRemoteSwitchApiIsOffline() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        int port = server.getAddress().getPort();
        server.stop(0);

        SwitchClient client = new SwitchClient(RestClient.builder().build(), "integration-key");
        Habitacion habitacion = new Habitacion(
                "room-1",
                "Room 1",
                21.0,
                1.0,
                "thermostat-1",
                "integration/room-1",
                "switch-1",
                "http://127.0.0.1:" + port,
                20.0,
                EstadoSwitch.APAGADO);

        assertThrows(ResourceAccessException.class, () -> client.encender(habitacion));
    }

    @Test
    void reportsServiceUnavailableWhenRemoteSwitchApiFails() throws Exception {
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
            Habitacion habitacion = new Habitacion(
                    "room-1",
                    "Room 1",
                    21.0,
                    1.0,
                    "thermostat-1",
                    "integration/room-1",
                    "switch-1",
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    20.0,
                    EstadoSwitch.APAGADO);

            HttpServerErrorException.ServiceUnavailable error = assertThrows(
                    HttpServerErrorException.ServiceUnavailable.class,
                    () -> client.encender(habitacion));

            assertEquals(503, error.getStatusCode().value());
            assertEquals(1, receivedRequests.get(), "The REST request should reach the switch API");
        } finally {
            server.stop(0);
        }
    }
}
