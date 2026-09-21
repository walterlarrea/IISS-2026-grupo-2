package org.subs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subs.dto.MessageObject.MessageObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.subs.controllertemp.ControllerTempAuto;

@Service
public class MqttSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriber.class);
    private final MongoTemperatureWriter mongoWriter;
    private final ControllerTempAuto controllerTempAuto;
    private final ObjectMapper objMap;
    private final String broker;
    private final String clientId;
    private final String topic;

    public MqttSubscriber(MongoTemperatureWriter mongoWriter, ControllerTempAuto controllerTempAuto, ObjectMapper objMap, @Value("${mqtt.broker-url}") String broker, @Value("${mqtt.client-id}") String clientId, @Value("${mqtt.topic}") String topic) {
        this.mongoWriter = mongoWriter;
        this.controllerTempAuto = controllerTempAuto;
        this.objMap = objMap;
        this.broker = broker;
        this.clientId = clientId;
        this.topic = topic;
    }

    void processMessage(String payload) {mongoWriter.saveTemperature(payload);}

    private void initiateConnection() {
        int qos = 1;
        try {
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

            // Set callback to handle incoming messages
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.error("Connection lost: " + cause.getMessage());
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    logger.info("Topic: " + topic + " | Message: " + payload);
                    try {
                        processMessage(payload);
                        logger.info("'Medición' saved in MongoDB");
                        MessageObject.Temperatura temp = objMap.readValue(payload, MessageObject.Temperatura.class);
                        controllerTempAuto.controlarHab(temp.tC(), String.valueOf(temp.id()));
                        logger.info("Controller for room id = {}",temp.id());
                    } catch (Exception e) {
                        logger.error("Failed saving 'Medición' in MongoDB");
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

            logger.info("Connecting to broker: " + broker);
            client.connect(connOpts);
            logger.info("Connected!");

            // Subscribe to the topic
            client.subscribe(topic, qos);
            logger.info("Subscribed to topic: " + topic);

        } catch (MqttException me) {
            logger.error("reason " + me.getReasonCode());
            logger.error("msg " + me.getMessage());
            logger.error("loc " + me.getLocalizedMessage());
            logger.error("cause " + me.getCause());
            me.printStackTrace();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {initiateConnection();}
}
