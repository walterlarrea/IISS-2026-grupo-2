package com.iiss.sensores.grupo_2.controlador.engine.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.CoreFactory;
import com.iiss.sensores.grupo_2.ICore;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.writer.MongoTemperatureWriter;
import com.iiss.sensores.grupo_2.dto.MessageObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MqttSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriber.class);
    private final MongoTemperatureWriter mongoWriter;
    private final ControllerTempAuto controllerTempAuto;
    private final ObjectMapper objMap;
    private final String broker;
    private final String clientId;
    private final String topic;

    public MqttSubscriber(
        MongoTemperatureWriter mongoWriter,
        ControllerTempAuto controllerTempAuto,
        ObjectMapper objMap,
        @Value("${mqtt.broker-url:tcp://localhost:1883}") String broker,
        @Value("${mqtt.client-id:ControladorSubscriber}") String clientId,
        @Value("${mqtt.topic:sensores/temperatura}") String topic
    ) {
        this.mongoWriter = mongoWriter;
        this.controllerTempAuto = controllerTempAuto;
        this.objMap = objMap;
        this.broker = broker;
        this.clientId = clientId;
        this.topic = topic;

        ICore core = CoreFactory.getCore();
    }

    public void processMessage(String payload) {
        mongoWriter.saveTemperature(payload);
    }

    private void initiateConnection() {
        int qos = 1;
        try {
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.error("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());

                    logger.info("Mensaje MQTT recibido. Topic: {} | Payload: {}", topic, payload);

                    try {
                        MessageObject.Temperatura temp =
                                objMap.readValue(payload, MessageObject.Temperatura.class);

                        logger.info(
                                "Temperatura recibida: {} °C. Termostato: {}",
                                temp.tC(),
                                temp.id()
                        );

                        try {
                            processMessage(payload);
                            logger.info("Medición guardada en MongoDB");
                        } catch (Exception e) {
                            logger.error("Error al guardar la medición en MongoDB", e);
                        }

                        try {
                            logger.info("Iniciando control de temperatura para {}", temp.id());

                            controllerTempAuto.controlarHab(temp.tC(), temp.id());

                            logger.info("Finalizó el procesamiento de temperatura para {}", temp.id());
                        } catch (Exception e) {
                            logger.error(
                                    "Error al controlar la temperatura del termostato {}",
                                    temp.id(),
                                    e
                            );
                        }

                    } catch (Exception e) {
                        logger.error("Error al interpretar el mensaje MQTT: {}", payload, e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // No se utiliza en el subscriber
                }
            });

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            logger.info("Connecting to broker: " + broker);
            client.connect(connOpts);
            logger.info("Connected!");

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
    public void start() {
        initiateConnection();
    }
}
