package org.example;

public class Main {
    static final String mqttTopic = System.getenv().getOrDefault("MQTT_TOPIC", "home/sensors/temperature");

    public static void main(String[] args) {
        Factory factory = Factory.create();
        SensorProcessor sensorProcessor = new SensorProcessor(mqttTopic, factory.getMqttSubscriber());
        sensorProcessor.start();
    }
}
