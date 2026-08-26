package org.example;
import java.util.Random;

public class EventGenerator {

    private static final AppConfig appConfig = AppConfig.getInstance();

    private final MqttPublisher publisher = new MqttPublisher(appConfig.getValues().get(appConfig.MQTT_BROKER_URL), 
    appConfig.getValues().get(appConfig.MQTT_PUBLISHER_ID), appConfig.getValues().get(appConfig.MQTT_ROOM1));

    private final MqttPublisher publisher2 = new MqttPublisher(appConfig.getValues().get(appConfig.MQTT_BROKER_URL),
    appConfig.getValues().get(appConfig.MQTT_PUBLISHER_ID2), appConfig.getValues().get(appConfig.MQTT_ROOM2));

    private final MqttPublisher publisher3 = new MqttPublisher(appConfig.getValues().get(appConfig.MQTT_BROKER_URL),
    appConfig.getValues().get(appConfig.MQTT_PUBLISHER_ID3), appConfig.getValues().get(appConfig.MQTT_ROOM3)); 
    
    private static final Random random = new Random();
    private Float temperature = (float) (random.nextInt(26));
    private Float temperature2 = (float) (random.nextInt(26));
    private Float temperature3 = (float) (random.nextInt(26));
    
    private float generateTemperature(float temperature) {

    float variation = (random.nextInt(7) - 3) / 10.0f;

    temperature += variation;

    return temperature;
}

private String createMessage(float temperature) {

    double tF = temperature * 9.0 / 5.0 + 32.0;
    double ts = System.currentTimeMillis() / 1000.0;

    return String.format("{\"id\":0,\"tC\":%.1f,\"tF\":%.1f,\"ts\":%.3f}",temperature, tF, ts);
}

public void generate() {

    while (true) {

        temperature = generateTemperature(temperature);
        temperature2 = generateTemperature(temperature2);
        temperature3 = generateTemperature(temperature3);

        publisher.publish(createMessage(temperature));
        publisher2.publish(createMessage(temperature2));
        publisher3.publish(createMessage(temperature3));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
}

}