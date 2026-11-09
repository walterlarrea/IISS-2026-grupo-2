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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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

  private static Room room1() {
    return new Room(
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
  }

  @Test
  void whenSwitchStateIsUnknownSendsOffCommand() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "simulator-key");

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"DESCONOCIDO","aplicadoEn":"2026-10-12T00:00:00Z"}
                        """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andExpect(method(POST))
        .andExpect(header("X-API-Key", "simulator-key"))
        .andExpect(
            content()
                .json(
                    """
                        {"accion":"OFF"}
                        """))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"OFF","cambio":true,"aplicadoEn":"2026-10-12T00:00:01Z"}
                        """,
                MediaType.APPLICATION_JSON));
    expectGetState(server, "OFF");

    assertEquals(SwitchState.APAGADO, client.consultarEstado(room1()).estado());

    server.verify();
  }

  @Test
  void whenOffCommandForUnknownSwitchFailsStillReportsApagado() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"DESCONOCIDO","aplicadoEn":"2026-10-12T00:00:00Z"}
                        """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andExpect(method(POST))
        .andRespond(withServerError());

    assertEquals(SwitchState.APAGADO, client.consultarEstado(room1()).estado());

    server.verify();
  }

  @Test
  void aSwitchThatFailedIsNotCalledAgainDuringTheBackoff() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");

    // Only ONE request is expected: the second attempt must not reach the switch
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andRespond(withServerError());

    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> client.apagar(room1()));
    org.junit.jupiter.api.Assertions.assertThrows(
        SwitchClient.SwitchInBackoffException.class, () -> client.apagar(room1()));

    server.verify();
  }

  @Test
  void resetBackoffAllowsAnImmediateRetry() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andRespond(withServerError());
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"OFF","cambio":true}
                        """,
                MediaType.APPLICATION_JSON));
    expectGetState(server, "OFF");

    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> client.apagar(room1()));
    client.resetBackoff();
    client.apagar(room1());

    server.verify();
  }

  private static void expectPostOff(MockRestServiceServer server) {
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andExpect(method(POST))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"OFF","cambio":true}
                        """,
                MediaType.APPLICATION_JSON));
  }

  private static void expectGetState(MockRestServiceServer server, String estado) {
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "{\"idSwitch\":\"sw-sim-room1\",\"estado\":\"" + estado + "\"}",
                MediaType.APPLICATION_JSON));
  }

  /** F05: the switch answers OK to the OFF but stays ON; only the GET tells the truth */
  @Test
  void anOffAcceptedButStillOnIsAFailureAndGoesToBackoff() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");

    expectPostOff(server);
    expectGetState(server, "ON");

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, () -> client.apagar(room1()));
    org.junit.jupiter.api.Assertions.assertThrows(
        SwitchClient.SwitchInBackoffException.class, () -> client.apagar(room1()));

    server.verify();
  }

  /** F08: a switch that can't be queried is trusted on the command's answer */
  @Test
  void whenTheOffCannotBeConfirmedTheCommandAnswerIsTrusted() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");

    expectPostOff(server);
    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1"))
        .andExpect(method(GET))
        .andRespond(withServerError());

    client.apagar(room1());

    server.verify();
  }

  /** Clock whose instant the test moves by hand */
  private static final class ManualClock extends java.time.Clock {
    private volatile java.time.Instant instant;

    ManualClock(java.time.Instant instant) {
      this.instant = instant;
    }

    void advance(java.time.Duration d) {
      instant = instant.plus(d);
    }

    @Override
    public java.time.ZoneId getZone() {
      return java.time.ZoneOffset.UTC;
    }

    @Override
    public java.time.Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public java.time.Instant instant() {
      return instant;
    }
  }

  /** With factor 60, one real second is a simulated minute: past the 30 s backoff */
  @Test
  void theBackoffIsMeasuredWithTheSimulatedClock() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");
    ManualClock real = new ManualClock(java.time.Instant.parse("2026-11-08T10:00:00Z"));
    client.setClock(
        new com.iiss.sensores.grupo_2.controlador.engine.utils.CustomClock(
            java.time.Instant.parse("2026-11-03T13:00:00Z"), 60, real));

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andRespond(withServerError());
    expectPostOff(server);
    expectGetState(server, "OFF");

    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> client.apagar(room1()));
    real.advance(java.time.Duration.ofMillis(400)); // 24 s simulated: still in backoff
    org.junit.jupiter.api.Assertions.assertThrows(
        SwitchClient.SwitchInBackoffException.class, () -> client.apagar(room1()));
    real.advance(java.time.Duration.ofMillis(200)); // 36 s simulated: retried
    client.apagar(room1());

    server.verify();
  }

  /** Repeated failures never wait more than 2 simulated minutes */
  @Test
  void theBackoffIsCappedAtTwoSimulatedMinutes() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");
    ManualClock real = new ManualClock(java.time.Instant.parse("2026-11-08T10:00:00Z"));
    client.setClock(
        new com.iiss.sensores.grupo_2.controlador.engine.utils.CustomClock(
            java.time.Instant.parse("2026-11-03T13:00:00Z"), 60, real));

    for (int i = 0; i < 6; i++) {
      server
          .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
          .andRespond(withServerError());
    }
    expectPostOff(server);
    expectGetState(server, "OFF");

    for (int i = 0; i < 6; i++) {
      org.junit.jupiter.api.Assertions.assertThrows(
          org.springframework.web.client.HttpServerErrorException.class,
          () -> client.apagar(room1()));
      real.advance(java.time.Duration.ofSeconds(2)); // 2 simulated minutes
    }
    client.apagar(room1());

    server.verify();
  }

  @Test
  void anOffAnsweredWithAnOnStateIsAFailure() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"ON","cambio":false}
                        """,
                MediaType.APPLICATION_JSON));

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, () -> client.apagar(room1()));
  }

  @Test
  void knownSwitchStatesDoNotSendCommands() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SwitchClient client = new SwitchClient(builder.build(), "");

    server
        .expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                """
                        {"idSwitch":"sw-sim-room1","estado":"OFF","aplicadoEn":"2026-10-12T00:00:00Z"}
                        """,
                MediaType.APPLICATION_JSON));

    client.consultarEstado(room1());

    // verify() fails if any unexpected request (e.g. a POST command) was made
    server.verify();
  }
}
