package com.iiss.sensores.grupo_2.controlador.engine.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.dto.ThermostatMessageObject;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class MqttSubscriber {
  private static final Logger logger = LoggerFactory.getLogger(MqttSubscriber.class);
  private final TemperatureReadingRepository tempMeasurements;
  private final ControllerTempAuto controllerTempAuto;
  private final ObjectMapper objMap;
  private final Set<String> desiredTopics = new HashSet<>();
  private final Set<String> subscribedTopics = new HashSet<>();
  private MqttClient client;

  public MqttSubscriber(
      TemperatureReadingRepository mongoWriter,
      ControllerTempAuto controllerTempAuto,
      ObjectMapper objMap) {
    this.tempMeasurements = mongoWriter;
    this.controllerTempAuto = controllerTempAuto;
    this.objMap = objMap;
  }

  public synchronized void connect(String broker, String clientId, Collection<String> topics) {
    Set<String> topicsToSubscribe = validateTopics(topics);
    disconnect();
    desiredTopics.clear();
    desiredTopics.addAll(topicsToSubscribe);
    try {
      client = new MqttClient(broker, clientId, new MemoryPersistence());

      client.setCallback(
          new MqttCallbackExtended() {
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
                  applySubscriptions();
                } catch (RuntimeException exception) {
                  logger.error(
                      "No se pudieron restaurar las suscripciones MQTT tras conectar", exception);
                }
              }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
              String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

              logger.info("Mensaje MQTT recibido. Topic: {} | Payload: {}", topic, payload);

              try {
                ThermostatMessageObject.Temperature parsedThermostatMessage =
                    objMap.readValue(payload, ThermostatMessageObject.Temperature.class);

                // Get the first section of the topic, eg: ht-room1/status/temperature:0
                // Being "ht-room1" the thermostat ID
                String idThermostat = topic.split("/")[0];

                TemperatureMessage tempMessage =
                    new TemperatureMessage(
                        parsedThermostatMessage.id(),
                            idThermostat,
                        parsedThermostatMessage.tC(),
                        parsedThermostatMessage.tF(),
                        Instant.ofEpochMilli(
                            parsedThermostatMessage.ts().movePointRight(3).longValueExact()));

                logger.info(
                    "Temperature received: {} °C. Thermostat: {}", tempMessage.tC(), idThermostat);

                try {
                  tempMeasurements.saveTemperature(tempMessage);
                  logger.info("Medición guardada en MongoDB");
                } catch (Exception e) {
                  logger.error("Error al guardar la medición en MongoDB", e);
                }

                try {
                  logger.info("Procesando nueva medición del termostato {}", idThermostat);

                  controllerTempAuto.processTemperature(tempMessage);

                } catch (Exception e) {
                  logger.error(
                      "Error al procesar la temperatura del termostato {}", idThermostat, e);
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

      applySubscriptions();

    } catch (MqttException me) {
      disconnect();
      throw new IllegalStateException(
          "No se pudo conectar o suscribir al broker MQTT " + broker, me);
    } catch (RuntimeException exception) {
      disconnect();
      throw exception;
    }
  }

  public synchronized void updateSubscriptions(Collection<String> topics) {
    Set<String> topicsToSubscribe = validateTopics(topics);
    desiredTopics.clear();
    desiredTopics.addAll(topicsToSubscribe);
    applySubscriptions();
  }

  private Set<String> validateTopics(Collection<String> topics) {
    if (topics == null || topics.stream().anyMatch(topic -> topic == null || topic.isBlank())) {
      throw new IllegalArgumentException(
          "Los topics MQTT deben ser una colección de valores no vacíos");
    }
    return new HashSet<>(topics);
  }

  private void applySubscriptions() {
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
        throw new IllegalStateException(
            "No se pudo cancelar la suscripción MQTT al topic " + topic, exception);
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

  private void disconnect() {
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
  public synchronized void close() {
    disconnect();
  }
}
