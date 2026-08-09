package org.example;

public class Factory {
    private final MqttSubscriber mqttSubscriber;

    public Factory(String topic, MqttSubscriber mqttSubscriber) {
        this.mqttSubscriber = mqttSubscriber;
    }


    public MqttSubscriber getMqttSubscriber() {
        return mqttSubscriber;
    }

    public static Factory create() {
        String broker = System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://localhost:1883");
        String clientId = System.getenv().getOrDefault("MQTT_CLIENT_ID", "JavaSubscriber");

        MqttSubscriber mqttSubscriber = new MqttSubscriber(broker, clientId);
        return new Factory(topic, mqttSubscriber);
    }
}
