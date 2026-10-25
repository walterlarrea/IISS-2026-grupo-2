package com.iiss.sensores.grupo_2.controlador.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Testcontainers
@Tag("Layer1")
class MqttResilienceL1Test {
    private static final int MQTT_PORT = 1883;
    private static final String MOSQUITTO_CONFIG = "/mosquitto/config/mosquitto.conf";
    private static final String SUPERVISOR_COMMAND =
            "while true; do mosquitto -c " + MOSQUITTO_CONFIG
                    + "; while [ -f /tmp/mosquitto-stopped ]; do sleep 0.1; done; done";

    @Container
    private final GenericContainer<?> broker = new GenericContainer<>("eclipse-mosquitto:2.0.22")
            .withExposedPorts(MQTT_PORT)
            .withCommand("sh", "-c", SUPERVISOR_COMMAND)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mosquitto.conf"),
                    MOSQUITTO_CONFIG)
            .waitingFor(Wait.forListeningPort());

    @Test
    void subscriberReconnectsAndReceivesMessagesAfterBrokerRestart() throws Exception {
        // Comprueba que el suscriptor recupera su conexión y recibe mensajes tras reiniciar el broker.
        TemperatureReadingRepository repository = mock(TemperatureReadingRepository.class);
        ControllerTempAuto controller = mock(ControllerTempAuto.class);
        MqttSubscriber subscriber = new MqttSubscriber(repository, controller, new ObjectMapper());
        String topic = "integration/recovery/" + UUID.randomUUID();
        String clientId = "recovery-" + UUID.randomUUID();
        String brokerUri = brokerUri();

        try {
            subscriber.connect(brokerUri, clientId, List.of(topic));
            awaitSubscription(clientId, 1);

            restartBroker();
            await().atMost(Duration.ofSeconds(30)).until(this::brokerAcceptsMqttConnections);
            awaitSubscription(clientId, 2);

            MqttClient publisher = publisher(brokerUri);
            try {
                await().atMost(Duration.ofSeconds(30))
                        .pollInterval(Duration.ofMillis(500))
                        .untilAsserted(() -> {
                            publish(publisher, topic, "recovered-message");
                            verify(repository).saveTemperature(
                                    argThat(message -> "recovered-message".equals(message.id())));
                        });
            } finally {
                closePublisher(publisher);
            }
        } finally {
            subscriber.close();
        }
    }

    @Test
    void messagesPublishedWhileSubscriberIsOfflineAreLostButNewMessagesArrive() throws Exception {
        // Comprueba que los mensajes enviados sin suscriptor no se reproducen y que los nuevos sí llegan.
        TemperatureReadingRepository repository = mock(TemperatureReadingRepository.class);
        ControllerTempAuto controller = mock(ControllerTempAuto.class);
        MqttSubscriber subscriber = new MqttSubscriber(repository, controller, new ObjectMapper());
        String topic = "integration/loss/" + UUID.randomUUID();
        String clientId = "loss-" + UUID.randomUUID();
        String brokerUri = brokerUri();

        MqttClient publisher = publisher(brokerUri);
        try {
            subscriber.connect(brokerUri, clientId, List.of(topic));
            awaitSubscription(clientId, 1);

            subscriber.close();
            publish(publisher, topic, "message-published-offline");

            subscriber.connect(brokerUri, clientId, List.of(topic));
            awaitSubscription(clientId, 2);

            await().atMost(Duration.ofSeconds(10))
                    .pollInterval(Duration.ofMillis(250))
                    .untilAsserted(() -> {
                        publish(publisher, topic, "message-after-reconnect");
                        verify(repository).saveTemperature(
                                argThat(message -> "message-after-reconnect".equals(message.id())));
                    });
            verify(repository, atLeastOnce()).saveTemperature(argThat(
                    message -> "message-after-reconnect".equals(message.id())));
            verify(repository, org.mockito.Mockito.never()).saveTemperature(argThat(
                    message -> "message-published-offline".equals(message.id())));
        } finally {
            subscriber.close();
            closePublisher(publisher);
        }
    }

    private String brokerUri() {
        return "tcp://" + broker.getHost() + ":" + broker.getMappedPort(MQTT_PORT);
    }

    private MqttClient publisher(String brokerUri) throws MqttException {
        MqttClient publisher = new MqttClient(
                brokerUri,
                "publisher-" + UUID.randomUUID(),
                new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        publisher.connect(options);
        return publisher;
    }

    private void publish(MqttClient publisher, String topic, String messageId) throws Exception {
        String payload = """
                {"id":"%s","tC":21.5,"tF":70.7,"ts":1720000000.000}
                """.formatted(messageId);
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        publisher.publish(topic, message);
    }

    private void awaitSubscription(String clientId, int expectedCount) {
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            String subscriptionLog = "Received SUBSCRIBE from " + clientId;
            String logs = broker.getLogs();
            return logs.split(Pattern.quote(subscriptionLog), -1).length - 1 >= expectedCount;
        });
    }

    private void restartBroker() throws Exception {
        var stopResult = broker.execInContainer(
                "sh", "-c", "touch /tmp/mosquitto-stopped && kill -TERM \"$(pidof mosquitto)\"");
        if (stopResult.getExitCode() != 0) {
            throw new IllegalStateException("No se pudo detener Mosquitto: " + stopResult.getStderr());
        }
        await().atMost(Duration.ofSeconds(10)).until(this::brokerProcessStopped);
        var startResult = broker.execInContainer(
                "sh", "-c", "rm -f /tmp/mosquitto-stopped");
        if (startResult.getExitCode() != 0) {
            throw new IllegalStateException("No se pudo reiniciar Mosquitto: " + startResult.getStderr());
        }
    }

    private boolean brokerProcessStopped() {
        try {
            return broker.execInContainer("sh", "-c", "pidof mosquitto").getExitCode() != 0;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo consultar el proceso Mosquitto", exception);
        }
    }

    private boolean brokerAcceptsMqttConnections() {
        MqttClient probe = null;
        try {
            probe = publisher(brokerUri());
            return true;
        } catch (MqttException exception) {
            return false;
        } finally {
            if (probe != null) {
                try {
                    closePublisher(probe);
                } catch (MqttException exception) {
                    throw new IllegalStateException("No se pudo cerrar la conexión MQTT de prueba", exception);
                }
            }
        }
    }

    private void closePublisher(MqttClient publisher) throws MqttException {
        if (publisher.isConnected()) {
            publisher.disconnect();
        }
        publisher.close();
    }

}
