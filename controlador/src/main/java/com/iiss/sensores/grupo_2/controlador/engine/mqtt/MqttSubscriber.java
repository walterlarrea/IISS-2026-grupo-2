package com.iiss.sensores.grupo_2.controlador.engine.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.CoreFactory;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.writer.MongoTemperatureWriter;
import com.iiss.sensores.grupo_2.dto.MessageObject;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class MqttSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriber.class);
    private final MongoTemperatureWriter mongoWriter;
    private final ControllerTempAuto controllerTempAuto;
    private final ObjectMapper objMap;
    private MqttClient client;

    public MqttSubscriber(
        MongoTemperatureWriter mongoWriter,
        ControllerTempAuto controllerTempAuto,
        ObjectMapper objMap
    ) {
        this.mongoWriter = mongoWriter;
        this.controllerTempAuto = controllerTempAuto;
        this.objMap = objMap;
    }

    public void processMessage(String payload) {
        mongoWriter.saveTemperature(payload);
    }

    public synchronized void conectar(
            String broker,
            String clientId,
            String topic
    ) {
        desconectar();
        try {
            client = new MqttClient(broker, clientId, new MemoryPersistence());

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.error("Connection to MQTT broker was lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

                    logger.info("Mensaje MQTT recibido. Topic: {} | Payload: {}", topic, payload);

                    try {
                        MessageObject.Temperatura temp =
                                objMap.readValue(payload, MessageObject.Temperatura.class);
                        String idTermostato = temp.id();

                        logger.info(
                                "Temperatura recibida: {} °C. Termostato: {}",
                                temp.tC(),
                                idTermostato
                        );

                        try {
                            processMessage(payload);
                            logger.info("Medición guardada en MongoDB");
                        } catch (Exception e) {
                            logger.error("Error al guardar la medición en MongoDB", e);
                        }

                        try {
                            logger.info("Iniciando control de temperatura para {}", idTermostato);

                            controllerTempAuto.controlarHab(temp.tC(), idTermostato);

                            logger.info("Finalizó el procesamiento de temperatura para {}", idTermostato);
                        } catch (Exception e) {
                            logger.error(
                                    "Error al controlar la temperatura del termostato {}",
                                    idTermostato,
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
            connOpts.setAutomaticReconnect(true);

            logger.info("Connecting to broker: {}", broker);
            client.connect(connOpts);
            logger.info("Connected to MQTT broker");

            client.subscribe(topic, 1);
            logger.info("Subscribed to topic: {}", topic);

        } catch (MqttException me) {
            desconectar();
            throw new IllegalStateException("No se pudo conectar o suscribir al broker MQTT " + broker, me);
        }
    }

    private void desconectar() {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException exception) {
            logger.warn("Error al cerrar la conexión MQTT anterior", exception);
        } finally {
            client = null;
        }
    }

    @PreDestroy
    public synchronized void cerrar() {
        desconectar();
    }
}
