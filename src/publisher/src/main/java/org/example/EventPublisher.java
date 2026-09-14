package org.example;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class EventPublisher {
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
            IO.println("Error");
        }
    }
}
