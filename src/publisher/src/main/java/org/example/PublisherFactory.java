package org.example;

import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@Singleton
public class PublisherFactory {
    private MqttClient client;
//    private final AppConfig appConfig;

    @Inject
    public PublisherFactory(AppConfig appConfig) {
//        this.appConfig = appConfig;
        final String mqttUrl = appConfig.getValue(appConfig.MQTT_BROKER_URL);
        final String clientId = "Publisher";// appConfig.getValue(appConfig.MQTT_BROKER_URL);

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
