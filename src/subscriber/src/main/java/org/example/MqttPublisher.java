package org.example;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublisher {
//
//    private Logger logger = LoggerFactory.getLogger(MqttPublisher.class);
//
//    private MqttClient client;
//    private String topic;
//
//    public MqttPublisher(String broker, String clientId, String topic) {
//        this.topic = topic;
//        initiateConnection(broker, clientId);
//    }
//
//    private void initiateConnection(String broker, String clientId) {
//
//        try {
//            client = new MqttClient(broker, clientId);
//
//            MqttConnectOptions connOpts = new MqttConnectOptions();
//            connOpts.setCleanSession(true);
//
//            logger.info("Connecting to broker: {}", broker);
//
//            client.connect(connOpts);
//
//            logger.info("Connected!");
//
//        } catch (MqttException e) {
//            logger.error("Error connecting to MQTT broker", e);
//        }
//    }
//
//    public void publish(String message) {
//
//        try {
//            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
//            mqttMessage.setQos(1);
//
//            client.publish(topic, mqttMessage);
//
//            logger.info("Published to {}: {}", topic, message);
//
//        } catch (MqttException e) {
//            logger.error("Error publishing message", e);
//        }
//    }
}