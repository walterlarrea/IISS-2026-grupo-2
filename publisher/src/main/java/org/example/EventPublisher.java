package org.example;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    private final MqttClient client;
    private final String topic;

    public EventPublisher(MqttClient client, String topic){
        this.client = client;
        this.topic = topic;
    }

    public void publish(String content){
        try {
            MqttMessage message = new MqttMessage(content.getBytes(StandardCharsets.UTF_8));

            this.client.publish(this.topic, message);
        }catch(MqttException e){
            logger.error("Error publishing to topic " + this.topic + ": " + e.getMessage());
        }
    }
}
