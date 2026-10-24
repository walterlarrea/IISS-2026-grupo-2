package com.iiss.sensores.grupo_2.controlador.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Testcontainers
@Tag("Layer1")
class MqttResilienceL1Test {
    private static final int MQTT_PORT = 1883;
    private static final String MOSQUITTO_CONFIG = "/mosquitto/config/mosquitto.conf";

    @Container
    private final GenericContainer<?> broker = new GenericContainer<>("eclipse-mosquitto:2.0.22")
            .withExposedPorts(MQTT_PORT)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mosquitto.conf"),
                    MOSQUITTO_CONFIG)
            .waitingFor(Wait.forListeningPort());

    @Test
    void subscriberReconnectsAndReceivesMessagesAfterBrokerRestart() throws Exception {
        TemperatureReadingRepository repository = mock(TemperatureReadingRepository.class);
        ControllerTempAuto controller = mock(ControllerTempAuto.class);
        MqttSubscriber subscriber = new MqttSubscriber(repository, controller, new ObjectMapper());
        String topic = "integration/recovery/" + UUID.randomUUID();
        String clientId = "recovery-" + UUID.randomUUID();
        String brokerUri = brokerUri();

        try {
            subscriber.connect(brokerUri, clientId, List.of(topic));
            awaitSubscription(clientId, 1);

            stopBroker();
            await().atMost(Duration.ofSeconds(10)).until(() -> !brokerIsReachable());

            startBroker();
            await().atMost(Duration.ofSeconds(20)).until(this::brokerIsReachable);
            awaitSubscription(clientId, 2);

            try (MqttClient publisher = publisher(brokerUri)) {
                publish(publisher, topic, "recovered-message");
            }

            verify(repository, timeout(5_000)).saveTemperature(
                    argThat(message -> "recovered-message".equals(message.id())));
        } finally {
            subscriber.close();
            ensureBrokerStarted();
        }
    }

    @Test
    void messagesPublishedWhileSubscriberIsOfflineAreLostButNewMessagesArrive() throws Exception {
        TemperatureReadingRepository repository = mock(TemperatureReadingRepository.class);
        ControllerTempAuto controller = mock(ControllerTempAuto.class);
        MqttSubscriber subscriber = new MqttSubscriber(repository, controller, new ObjectMapper());
        String topic = "integration/loss/" + UUID.randomUUID();
        String clientId = "loss-" + UUID.randomUUID();
        String brokerUri = brokerUri();

        try (MqttClient publisher = publisher(brokerUri)) {
            subscriber.connect(brokerUri, clientId, List.of(topic));
            awaitSubscription(clientId, 1);

            subscriber.close();
            publish(publisher, topic, "message-published-offline");

            subscriber.connect(brokerUri, clientId, List.of(topic));
            awaitSubscription(clientId, 2);
            publish(publisher, topic, "message-after-reconnect");

            verify(repository, timeout(5_000)).saveTemperature(
                    argThat(message -> "message-after-reconnect".equals(message.id())));
            verify(repository).saveTemperature(argThat(
                    message -> "message-after-reconnect".equals(message.id())));
        } finally {
            subscriber.close();
        }
    }

    private String brokerUri() {
        return "tcp://" + broker.getHost() + ":" + broker.getMappedPort(MQTT_PORT);
    }

    private MqttClient publisher(String brokerUri) throws Exception {
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

    private boolean brokerIsReachable() {
        try (var socket = new java.net.Socket()) {
            socket.connect(
                    new java.net.InetSocketAddress(broker.getHost(), broker.getMappedPort(MQTT_PORT)),
                    250);
            return true;
        } catch (java.io.IOException exception) {
            return false;
        }
    }

    private void stopBroker() {
        DockerClientFactory.instance().client()
                .stopContainerCmd(broker.getContainerId())
                .withTimeout(5)
                .exec();
    }

    private void startBroker() {
        DockerClientFactory.instance().client()
                .startContainerCmd(broker.getContainerId())
                .exec();
    }

    private void ensureBrokerStarted() {
        var container = DockerClientFactory.instance().client()
                .inspectContainerCmd(broker.getContainerId())
                .exec();
        if (!Boolean.TRUE.equals(container.getState().getRunning())) {
            startBroker();
        }
    }
}
