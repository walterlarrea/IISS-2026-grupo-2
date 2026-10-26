package com.iiss.sensores.grupo_2.controlador.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BigBangSystemIntegrationIT {

  private static final String CTRL_SERVICE = "controlador";
  private static final String SWITCHES_SERVICE = "switches-api";
  private static final String MOSQUITTO_SERVICE = "mosquitto";

  private static final int CTRL_PORT = 8080;
  private static final int SWITCH_PORT = 8090;
  private static final int MQTT_PORT = 1883;

  // References the docker-compose-test.yml and the newly created .env.test
  @Container
  public static ComposeContainer environment =
      new ComposeContainer(new File("../docker/docker-compose-test.yml"))
          //     .withEnvFile(new File("../.env.test"))
          .withExposedService(
              CTRL_SERVICE,
              CTRL_PORT,
              Wait.forHttp("/control")
                  .withMethod("GET")
                  .withHeader("X-API-KEY", "test-ctrl-key")
                  .forStatusCode(200))
          .withExposedService(
              SWITCHES_SERVICE,
              SWITCH_PORT,
              Wait.forHttp("/switches/sw1").withMethod("GET").forStatusCode(200))
          .withExposedService(MOSQUITTO_SERVICE, MQTT_PORT, Wait.forListeningPort());

  //          .withLocalCompose(true);

  private RestClient restClient;
  private ObjectMapper objectMapper;
  private String controladorBaseUrl;
  private String switchesBaseUrl;
  private String mqttBrokerUrl;

  @BeforeEach
  void setup() {
    objectMapper = new ObjectMapper();
    restClient = RestClient.builder().build();

    String ctrlHost = environment.getServiceHost(CTRL_SERVICE, CTRL_PORT);
    int ctrlPort = environment.getServicePort(CTRL_SERVICE, CTRL_PORT);
    controladorBaseUrl = "http://" + ctrlHost + ":" + ctrlPort;

    String switchHost = environment.getServiceHost(SWITCHES_SERVICE, SWITCH_PORT);
    int switchPort = environment.getServicePort(SWITCHES_SERVICE, SWITCH_PORT);
    switchesBaseUrl = "http://" + switchHost + ":" + switchPort;

    String mqttHost = environment.getServiceHost(MOSQUITTO_SERVICE, MQTT_PORT);
    int mqttPort = environment.getServicePort(MOSQUITTO_SERVICE, MQTT_PORT);
    mqttBrokerUrl = "tcp://" + mqttHost + ":" + mqttPort;
  }

  @Test
  @Order(1)
  @DisplayName(
      "Big-Bang E2E Flow: Configure Site -> Emit MQTT Telemetry -> Automatic Core Evaluation -> Switch Actuated")
  void fullSystemEndToEndFlowIT() throws Exception {
    // 1. CONFIGURE ROOM & SITE via Controlador REST API
    String sitePayload =
        """
        {
          "schemaVersion": "2.0",
          "sitio": {
            "id": "sitio-1",
            "nombre": "Casa Test",
            "potenciaContratadaKW": 5.0,
            "tarifa": {
              "punta": {
                "desde": "18:00",
                "hasta": "22:00",
                "dias": "HABILES"
              }
            }
          },
          "habitaciones": [
            {
              "id": "room-0",
              "nombre": "Living Test",
              "temperaturaEsperada": 22.0,
              "potenciaKW": 1.5,
              "idTermostato": "t0",
              "topicTermostato": "t0/status/temperature:0",
              "idSwitch": "sw0",
              "urlSwitch": "http://switches-api:8090"
            }
          ]
        }
        """;

    ResponseEntity<Void> configResponse =
        restClient
            .put()
            .uri(controladorBaseUrl + "/sitio")
            .header("X-API-KEY", "test-ctrl-key")
            .contentType(MediaType.APPLICATION_JSON)
            .body(sitePayload)
            .retrieve()
            .toBodilessEntity();

    assertEquals(HttpStatus.OK, configResponse.getStatusCode());

    // 2. START THE AUTOMATIC ENGINE CONTROLLER
    restClient
        .post()
        .uri(controladorBaseUrl + "/control/comandos")
        .header("X-API-KEY", "test-ctrl-key")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            "{\"accion\": \"INICIAR\", \"fechaHora\": \"2026-10-25T14:00:00Z\", \"factorTiempo\": 1}")
        .retrieve()
        .toBodilessEntity();

    // 3. EMIT COLD TEMPERATURE TELEMETRY OVER MQTT (Simulating sensor / publisher)
    // Set temp to 16.0°C (target is 22.0°C) -> Should trigger Switch ON
    MqttClient testMqttPublisher =
        new MqttClient(mqttBrokerUrl, "BigBangTestSensor", new MemoryPersistence());
    testMqttPublisher.connect();

    String tempPayload =
        """
        {
          "id": "t0",
          "tC": 16.0,
          "tF": 60.8,
          "ts": 1789351400.000
        }
        """;

    MqttMessage message =
        new MqttMessage(tempPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    message.setQos(1);
    testMqttPublisher.publish("t0/status/temperature:0", message);
    testMqttPublisher.disconnect();
    testMqttPublisher.close();

    // 4. AWAITILITY ASSERTION: Validate that Switches-API was triggered by the Controlador
    // The flow crosses: Mosquitto -> Controlador Subscriber -> Mongo -> Core -> SwitchClient ->
    // Switches-API
    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(
            () -> {
              Map response =
                  restClient
                      .get()
                      .uri(switchesBaseUrl + "/switches/sw0")
                      .header("X-API-Key", "test-switch-key")
                      .retrieve()
                      .body(Map.class);

              assertNotNull(response);
              assertEquals(
                  "ON",
                  response.get("estado"),
                  "Switch sw0 should have been turned ON by the automatic engine");
            });
  }
}
