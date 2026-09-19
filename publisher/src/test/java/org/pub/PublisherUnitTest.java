package org.pub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.pub.dto.MessageObject.MessageObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PublisherUnitTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void validateJsonSerializesTemperaturaToJson() throws Exception {
        MessageObject.Temperatura temperatura = new MessageObject.Temperatura(7, 24.0f, 75.5f, 1789351400L);

        String json = MessageObject.validateJson(temperatura);
        JsonNode root = mapper.readTree(json);

        assertEquals(7, root.get("id").asInt());
        assertEquals(24.0, root.get("tC").asDouble(), 0.0001);
        assertEquals(75.5, root.get("tF").asDouble(), 0.0001);
        assertEquals(1789351400L, root.get("ts").asLong());
    }

    @Test
    void customSerializerRoundsTemperatureValuesToTwoDecimals() throws Exception {
        MessageObject.Temperatura temperatura = new MessageObject.Temperatura(3, 12.345f, 54.321f, 1789351400L);

        String json = MessageObject.validateJson(temperatura);

        assertTrue(json.contains("\"tC\":12.35"));
        assertTrue(json.contains("\"tF\":54.32"));
        assertTrue(json.contains("\"ts\":1789351400"));
    }

    @Test
    void epochTimeIsSerializedAsAnInteger() throws Exception {
        MessageObject.Temperatura temperatura = new MessageObject.Temperatura(1, 24.65f, 0.0f, 1789351400L);

        String json = MessageObject.validateJson(temperatura);

        assertTrue(json.contains("\"ts\":1789351400"));
        assertFalse(json.contains("E"));
    }

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

    @Test
    void eventPublisherPublishesPayloadToTheConfiguredTopic() throws Exception {
        MqttClient client = mock(MqttClient.class);
        EventPublisher publisher = new EventPublisher(client, "home/test/topic");

        publisher.publish("hello-from-junit");

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(MqttMessage.class);
        verify(client).publish(eq("home/test/topic"), messageCaptor.capture());
        assertEquals("hello-from-junit", new String(messageCaptor.getValue().getPayload(), StandardCharsets.UTF_8));
    }

    @Test
    void publisherFactoryConnectsToBrokerAndBuildsPublisher() throws Exception {
        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
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

    @Test
    void eventGeneratorEmitsAtLeastOneMessageBeforeInterruption() throws Exception {
        PublisherFactory publisherFactory = mock(PublisherFactory.class);
        EventPublisher publisher = mock(EventPublisher.class);
        AtomicInteger publishCalls = new AtomicInteger();

        when(publisherFactory.buildPublisher(anyString())).thenReturn(publisher);
        doAnswer(invocation -> {
            publishCalls.incrementAndGet();
            return null;
        }).when(publisher).publish(anyString());

        EventGenerator generator = new EventGenerator(publisherFactory);
        Method spamMethod = EventGenerator.class.getDeclaredMethod("spamEventMessage", EventPublisher.class, int.class);
        spamMethod.setAccessible(true);

        Thread thread = new Thread(() -> {
            try {
                spamMethod.invoke(generator, publisher, 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();

        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && publishCalls.get() == 0) {
            Thread.sleep(20);
        }

        assertTrue(publishCalls.get() > 0, "The generator should publish at least one message before interruption");

        thread.interrupt();
        thread.join(1000);
    }
}
