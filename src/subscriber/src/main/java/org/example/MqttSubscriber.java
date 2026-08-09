package org.example;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttSubscriber {

    private final String broker;
    private final String clientId;

    public MqttSubscriber(String broker, String clientId) {
        this.broker = broker;
        this.clientId = clientId;
    }

    public void start(String topic) {
        initiateConnection(broker, clientId, topic);
    }

    private void initiateConnection(String broker, String clientId, String topic) {
        int qos = 1;

        try {
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    System.out.println("Topic: " + topic + " Message: " + new String(message.getPayload()));
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
