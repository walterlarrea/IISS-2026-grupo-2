package com.iiss.sensores.grupo_2.pub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.iiss.sensores.grupo_2.dto.ThermostatMessageObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Prueba la serialización, configuración y publicación de mediciones MQTT. */
class PublisherUnitTest {

  private final ObjectMapper mapper = new ObjectMapper();

  /** Comprueba que una medición se convierta en un objeto JSON con sus campos. */
  @Test
  void validateJsonSerializesTemperaturaToJson() throws Exception {
    ThermostatMessageObject.Temperature temperature =
        new ThermostatMessageObject.Temperature(
            "ht-sim-room7", 24.0f, 75.5f, new BigDecimal("1789351400.000"));

    String json = ThermostatMessageObject.validateJson(temperature);
    JsonNode root = mapper.readTree(json);

    assertEquals("ht-sim-room7", root.get("id").asText());
    assertEquals(24.0, root.get("tC").asDouble(), 0.0001);
    assertEquals(75.5, root.get("tF").asDouble(), 0.0001);
    assertEquals(1789351400L, root.get("ts").asLong());
  }

  /** Comprueba que las temperaturas se serialicen con dos decimales. */
  @Test
  void customSerializerRoundsTemperatureValuesToTwoDecimals() throws Exception {
    ThermostatMessageObject.Temperature temperature =
        new ThermostatMessageObject.Temperature(
            "ht-sim-room3", 12.345f, 54.321f, new BigDecimal("1789351400.000"));

    String json = ThermostatMessageObject.validateJson(temperature);

    assertTrue(json.contains("\"tC\":12.35"));
    assertTrue(json.contains("\"tF\":54.32"));
    assertTrue(json.contains("\"ts\":1789351400"));
  }

  /** Comprueba que la marca de tiempo se serialice como un entero. */
  @Test
  void epochTimeIsSerializedAsAnInteger() throws Exception {
    ThermostatMessageObject.Temperature temperature =
        new ThermostatMessageObject.Temperature(
            "ht-sim-room1", 24.65f, 0.0f, new BigDecimal("1789351400.000"));

    String json = ThermostatMessageObject.validateJson(temperature);

    assertTrue(json.contains("\"ts\":1789351400"));
    assertFalse(json.contains("E"));
  }

  /** Comprueba que la configuración devuelva valores almacenados y null para claves ausentes. */
  @Test
  void appConfigReadsConfiguredValuesFromInternalMap() throws Exception {
    AppConfig appConfig = new AppConfig();
    Field valuesField = AppConfig.class.getDeclaredField("values");
    valuesField.setAccessible(true);

    @SuppressWarnings("unchecked")
    HashMap<String, String> values = (HashMap<String, String>) valuesField.get(appConfig);
    values.put("UNIT_TEST_KEY", "unit-test-value");

    assertEquals("unit-test-value", appConfig.getValue("UNIT_TEST_KEY"));
    assertNull(appConfig.getValue("MISSING_KEY"));
  }

  /** Comprueba que el publicador envíe el contenido al topic configurado. */
  @Test
  void eventPublisherPublishesPayloadToTheConfiguredTopic() throws Exception {
    MqttClient client = mock(MqttClient.class);
    EventPublisher publisher = new EventPublisher(client, "home/test/topic");

    publisher.publish("hello-from-junit");

    var messageCaptor = org.mockito.ArgumentCaptor.forClass(MqttMessage.class);
    verify(client).publish(eq("home/test/topic"), messageCaptor.capture());
    assertEquals(
        "hello-from-junit",
        new String(messageCaptor.getValue().getPayload(), StandardCharsets.UTF_8));
  }

  /** Comprueba que la fábrica conecte el cliente MQTT y lo cierre correctamente. */
  @Test
  void publisherFactoryConnectsToBrokerAndBuildsPublisher() throws Exception {
    try (MockedConstruction<MqttClient> mocked =
        mockConstruction(
            MqttClient.class,
            (mock, context) -> doNothing().when(mock).connect(any(MqttConnectOptions.class)))) {

      PublisherFactory factory = new PublisherFactory(new AppConfig());
      EventPublisher eventPublisher = factory.buildPublisher("home/test/topic");

      assertNotNull(eventPublisher);
      verify(mocked.constructed().get(0)).connect(any(MqttConnectOptions.class));

      factory.destroy();
      verify(mocked.constructed().get(0)).disconnect();
      verify(mocked.constructed().get(0)).close();
    }
  }

  /** Comprueba que el generador publique al menos un mensaje antes de interrumpirse. */
  @Test
  void eventGeneratorEmitsAtLeastOneMessageBeforeInterruption() throws Exception {
    PublisherFactory publisherFactory = mock(PublisherFactory.class);
    EventPublisher publisher = mock(EventPublisher.class);
    AtomicInteger publishCalls = new AtomicInteger();

    when(publisherFactory.buildPublisher(anyString())).thenReturn(publisher);
    doAnswer(
            invocation -> {
              publishCalls.incrementAndGet();
              return null;
            })
        .when(publisher)
        .publish(anyString());

    EventGenerator generator = new EventGenerator(publisherFactory);
    Method spamMethod =
        EventGenerator.class.getDeclaredMethod(
            "spamEventMessage", EventPublisher.class, String.class);
    spamMethod.setAccessible(true);

    Thread thread =
        new Thread(
            () -> {
              try {
                spamMethod.invoke(generator, publisher, "ht-sim-room0");
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });

    thread.start();

    long deadline = System.currentTimeMillis() + 2000;
    while (System.currentTimeMillis() < deadline && publishCalls.get() == 0) {
      Thread.sleep(20);
    }

    assertTrue(
        publishCalls.get() > 0,
        "The generator should publish at least one message before interruption");

    thread.interrupt();
    thread.join(1000);
  }
}
