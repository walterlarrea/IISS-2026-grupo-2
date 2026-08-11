package org.example;

public class Factory {
    public static void initiate() {
        AppConfig appConfig = AppConfig.getInstance();
        String TOPIC = appConfig.getValues().get(appConfig.MQTT_TOPIC);

        MqttSubscriber mqttSubscriber = MqttSubscriber.getInstance();
    }
}
