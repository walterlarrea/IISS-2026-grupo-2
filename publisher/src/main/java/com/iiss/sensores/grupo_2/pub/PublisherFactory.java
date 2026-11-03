package com.iiss.sensores.grupo_2.pub;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/** Administra el cliente MQTT compartido y crea publicadores por topic. */
@Component
public class PublisherFactory {
  private MqttClient client;

  /** Conecta el cliente MQTT usando la configuración de la aplicación. */
  public PublisherFactory(AppConfig appConfig) {
    final String mqttUrl = appConfig.getValue(appConfig.MQTT_BROKER_URL);
    // Genera un ID de client unico
    final String clientId = MqttClient.generateClientId();

    try {
      this.initClient(mqttUrl, clientId);
    } catch (MqttException e) {
      throw new RuntimeException(e);
    }
  }

  /** Crea el cliente MQTT y establece la conexión con el broker. */
  private void initClient(String mqttUrl, String clientId) throws MqttException {
    this.client = new MqttClient(mqttUrl, clientId, new MemoryPersistence());

    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(true);

    this.client.connect(options);
  }

  /** Devuelve un publicador vinculado al topic solicitado. */
  public EventPublisher buildPublisher(String topic) {
    return new EventPublisher(this.client, topic);
  }

  /** Desconecta y libera el cliente MQTT al cerrar la aplicación. */
  @PreDestroy
  public void destroy() throws MqttException {
    this.client.disconnect();
    this.client.close();
  }
}
