package org.example;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttSubscriber {
    private static final AppConfig appConfig = AppConfig.getInstance();

    private static final String BROKER_URL = appConfig.getValues().get(appConfig.MQTT_BROKER_URL);
    private static final String CLIENT_ID = appConfig.getValues().get(appConfig.MQTT_CLIENT_ID);
    private static final String TOPIC = appConfig.getValues().get(appConfig.MQTT_TOPIC);
    private final MongoTemperatureWriter mongoWriter;
    //private static final MqttSubscriber mqttSubscriber = new MqttSubscriber(BROKER_URL, CLIENT_ID, TOPIC, new MongoTemperatureWriter());
    private static MqttSubscriber mqttSubscriber;

    public static MqttSubscriber getInstance() {
        if (mqttSubscriber == null) {
            mqttSubscriber = new MqttSubscriber(BROKER_URL, CLIENT_ID, TOPIC, new MongoTemperatureWriter());
        }
        return mqttSubscriber;
    }
    // Se puede usar un cliendId aleatorio como este por ejemplo.
    // private static final MqttSubscriber mqttSubscriber = new MqttSubscriber(BROKER_URL, MqttClient.generateClientId(), TOPIC);

    private MqttSubscriber(String broker, String clientId, String topic, MongoTemperatureWriter mongoWriter) {
        this.mongoWriter = mongoWriter;
        initiateConnection(broker, clientId, topic);
    }

    MqttSubscriber(MongoTemperatureWriter mongoWriter) {this.mongoWriter = mongoWriter;}

    void processMessage(String payload) {mongoWriter.saveTemperature(payload);}

    private void initiateConnection(String broker, String clientId, String topic) {
        int qos = 1;
        try {
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

            // Set callback to handle incoming messages
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("Topic: " + topic + " | Message: " + payload);
                    try {
                        processMessage(payload);
                        System.out.println("Medición guardada en MongoDB");
                    } catch (Exception e) {
                        System.err.println("Error guardando medición en MongoDB");
                        e.printStackTrace();
                    }
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Unused for subscribers
                }
            });

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connected!");

            // Subscribe to the topic
            client.subscribe(topic, qos);
            System.out.println("Subscribed to topic: " + topic);

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            me.printStackTrace();
        }
    }
}
