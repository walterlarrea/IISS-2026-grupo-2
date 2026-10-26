package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.modelos.input.Room;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SwitchClientTest {

  @Test
  void sendsSimulatorCommandWithApiKeyAndContractPayload() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "simulator-key");

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andExpect(method(POST))
        .andExpect(header("X-API-Key", "simulator-key"))
        .andExpect(
            content()
                .json(
                    """
                        {"accion":"ON"}
                        """))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"ON","cambio":true,"aplicadoEn":"2026-10-12T20:00:00Z"}
                        """,
                MediaType.APPLICATION_JSON));

    Room room =
        new Room(
            "room1",
            "Room 1",
            22.0,
            1.5,
            "thermostat-room1",
            "topic-room1",
            "sw-sim-room1",
            "http://iiss-simulator:9090",
            21.0,
            SwitchState.APAGADO);

    client.encender(room);

    server.verify();
  }

  @Test
  void readsAndMapsTheSwitchStateWithApiKey() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "simulator-key");
    Room room =
        new Room(
            "room1",
            "Room 1",
            22.0,
            1.5,
            "thermostat-room1",
            "topic-room1",
            "sw-sim-room1",
            "http://iiss-simulator:9090",
            21.0,
            SwitchState.APAGADO);

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1"))
        .andExpect(method(GET))
        .andExpect(header("X-API-Key", "simulator-key"))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"ON","aplicadoEn":"2026-10-12T00:00:00Z"}
                        """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1"))
        .andExpect(method(GET))
        .andExpect(header("X-API-Key", "simulator-key"))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"OFF","aplicadoEn":"2026-10-12T00:00:00Z"}
                        """,
                MediaType.APPLICATION_JSON));

    assertEquals(SwitchState.ENCENDIDO, client.consultarEstado(room).estado());
    assertEquals(SwitchState.APAGADO, client.consultarEstado(room).estado());

    server.verify();
  }
}
