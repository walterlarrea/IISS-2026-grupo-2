package org.pub;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class PublisherFactory {
    private MqttClient client;

    public PublisherFactory(AppConfig appConfig) {
        final String mqttUrl = appConfig.getValue(appConfig.MQTT_BROKER_URL);
        // Genera un ID de client unico
        final String clientId = MqttClient.generateClientId();

        try {
            this.initClient(mqttUrl, clientId);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void initClient(String mqttUrl, String clientId) throws MqttException {
        this.client = new MqttClient(mqttUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        this.client.connect(options);
    }

    public EventPublisher buildPublisher(String topic) {
        return new EventPublisher(this.client, topic);
    }

    @PreDestroy
    public void destroy() throws MqttException {
        this.client.disconnect();
        this.client.close();
    }
}
