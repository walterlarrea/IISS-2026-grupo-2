package com.iiss.sensores.grupo_2.controlador.engine.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.MongoTemperatureWriter;
import com.iiss.sensores.grupo_2.dto.MessageObject;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class MqttSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriber.class);
    private final MongoTemperatureWriter mongoWriter;
    private final ControllerTempAuto controllerTempAuto;
    private final ObjectMapper objMap;
    private final Set<String> desiredTopics = new HashSet<>();
    private final Set<String> subscribedTopics = new HashSet<>();
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
        conectar(broker, clientId, Set.of(topic));
    }

    public synchronized void conectar(
            String broker,
            String clientId,
            Collection<String> topics
    ) {
        Set<String> topicsToSubscribe = validarTopics(topics);
        desconectar();
        desiredTopics.clear();
        desiredTopics.addAll(topicsToSubscribe);
        try {
            client = new MqttClient(broker, clientId, new MemoryPersistence());

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.error("Connection to MQTT broker was lost", cause);
                    synchronized (MqttSubscriber.this) {
                        subscribedTopics.clear();
                    }
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (!reconnect) {
                        return;
                    }
                    synchronized (MqttSubscriber.this) {
                        subscribedTopics.clear();
                        try {
                            aplicarSuscripciones();
                        } catch (RuntimeException exception) {
                            logger.error("No se pudieron restaurar las suscripciones MQTT tras conectar", exception);
                        }
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

                    logger.info("Mensaje MQTT recibido. Topic: {} | Payload: {}", topic, payload);

                    try {
                        MessageObject.Temperatura temp = objMap.readValue(payload, MessageObject.Temperatura.class);
                        String idTermostato = temp.id();

                        logger.info("Temperatura recibida: {} °C. Termostato: {}", temp.tC(), idTermostato);

                        try {
                            processMessage(payload);
                            logger.info("Medición guardada en MongoDB");
                        } catch (Exception e) {
                            logger.error("Error al guardar la medición en MongoDB", e);
                        }

                        try {
                            logger.info("Procesando nueva medición del termostato {}", idTermostato);

                            controllerTempAuto.procesarTemperatura(topic, temp.tC());

                        } catch (Exception e) {
                            logger.error("Error al procesar la temperatura del termostato {}", idTermostato, e);
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

            aplicarSuscripciones();

        } catch (MqttException me) {
            desconectar();
            throw new IllegalStateException("No se pudo conectar o suscribir al broker MQTT " + broker, me);
        } catch (RuntimeException exception) {
            desconectar();
            throw exception;
        }
    }

    public synchronized void actualizarSuscripciones(Collection<String> topics) {
        Set<String> topicsToSubscribe = validarTopics(topics);
        desiredTopics.clear();
        desiredTopics.addAll(topicsToSubscribe);
        aplicarSuscripciones();
    }

    private Set<String> validarTopics(Collection<String> topics) {
        if (topics == null || topics.stream().anyMatch(topic -> topic == null || topic.isBlank())) {
            throw new IllegalArgumentException("Los topics MQTT deben ser una colección de valores no vacíos");
        }
        return new HashSet<>(topics);
    }

    private void aplicarSuscripciones() {
        if (client == null || !client.isConnected()) {
            return;
        }

        Set<String> topicsToRemove = new HashSet<>(subscribedTopics);
        topicsToRemove.removeAll(desiredTopics);
        for (String topic : topicsToRemove) {
            try {
                client.unsubscribe(topic);
                subscribedTopics.remove(topic);
                logger.info("Unsubscribed from topic: {}", topic);
            } catch (MqttException exception) {
                throw new IllegalStateException("No se pudo cancelar la suscripción MQTT al topic " + topic, exception);
            }
        }

        Set<String> topicsToAdd = new HashSet<>(desiredTopics);
        topicsToAdd.removeAll(subscribedTopics);
        for (String topic : topicsToAdd) {
            try {
                client.subscribe(topic, 1);
                subscribedTopics.add(topic);
                logger.info("Subscribed to topic: {}", topic);
            } catch (MqttException exception) {
                throw new IllegalStateException("No se pudo suscribir al topic MQTT " + topic, exception);
            }
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
            subscribedTopics.clear();
        }
    }

    @PreDestroy
    public synchronized void cerrar() {
        desconectar();
    }
}
