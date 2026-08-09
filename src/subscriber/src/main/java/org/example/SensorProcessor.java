package org.example;

public class SensorProcessor {
    private final String topic;
    private final MqttSubscriber mqttSubscriber;

    public SensorProcessor(String topic, MqttSubscriber mqttSubscriber) {
        this.topic = topic;
        this.mqttSubscriber = mqttSubscriber;
    }

    public void start() {
        mqttSubscriber.start(topic);
    }
}
