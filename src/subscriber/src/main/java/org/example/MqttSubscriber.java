package org.example;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttSubscriber {

    private static final MqttSubscriber mqttSubscriber = new MqttSubscriber("tcp://mosquitto:1883", "JavaSubscriber", "home/sensors/temperature");

    private MqttSubscriber(String broker, String clientId, String topic) {
        initiateConnection(broker, clientId, topic);
    }

    public static MqttSubscriber getInstance() {
        return mqttSubscriber;
    }

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
                    System.out.println("Topic: " + topic +" | Message: " + new String(message.getPayload()));
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
