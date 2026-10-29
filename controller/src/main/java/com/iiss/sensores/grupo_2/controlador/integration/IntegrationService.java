package com.iiss.sensores.grupo_2.controlador.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicBoolean;

/** Dynamic register with MQTT component */
@Service
public class IntegrationService {
  private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);

  private final boolean conectarSimulador;
  private final String simulatorApiUrl;
  private final String simulatorApiKey;
  private final String controllerLabel;
  private final String controllerBaseUrl;
  private final String controllerApiKey;
  private final String fallbackBroker;
  private final String mqttClientId;
  private final RestClient simulatorClient;
  private final MqttSubscriptionService mqttSubscriptionService;
  private final AtomicBoolean initialized = new AtomicBoolean();

  public IntegrationService(
      RestClient.Builder restClientBuilder,
      MqttSubscriptionService mqttSubscriptionService,
      @Value("${CONECTAR_CON_SIMULADOR:false}") boolean conectarSimulador,
      @Value("${SIMULATOR_API_URL:}") String simulatorApiUrl,
      @Value("${SIMULATOR_API_KEY:}") String simulatorApiKey,
      @Value("${CONTROLADOR_ETIQUETA:controlador}") String controllerLabel,
      @Value("${CONTROLADOR_API_URL:http://localhost:8080}") String controllerBaseUrl,
      @Value("${CONTROLADOR_API_KEY:development-key}") String controllerApiKey,
      @Value("${MQTT_BROKER_URL:tcp://localhost:1883}") String fallbackBroker,
      @Value("${MQTT_CLIENT_ID:ControladorSubscriber}") String mqttClientId) {
    this.conectarSimulador = conectarSimulador;
    this.simulatorApiUrl = simulatorApiUrl;
    this.simulatorApiKey = simulatorApiKey;
    this.controllerLabel = controllerLabel;
    this.controllerBaseUrl = controllerBaseUrl;
    this.controllerApiKey = controllerApiKey;
    this.fallbackBroker = fallbackBroker;
    this.mqttClientId = mqttClientId;

    RestClient.Builder clientBuilder = restClientBuilder.clone();
    if (conectarSimulador) {
      logger.info(
          "Integración con simulador habilitada; se intentará registrar el controlador en {}",
          simulatorApiUrl);
      if (!simulatorApiUrl.isBlank()) {
        clientBuilder.baseUrl(simulatorApiUrl);
      }
      if (!simulatorApiKey.isBlank()) {
        clientBuilder.defaultHeader("X-API-Key", simulatorApiKey);
      }
    } else {
      logger.info(
          "Integración con simulador deshabilitada; se usará broker y topic MQTT propios: {} y {}",
          fallbackBroker);
    }

    this.simulatorClient = clientBuilder.build();
    this.mqttSubscriptionService = mqttSubscriptionService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (!conectarSimulador) {
      try {
        mqttSubscriptionService.conectar(fallbackBroker, mqttClientId);
        logger.info(
            "Integración con simulador deshabilitada; usando broker y topic configurados localmente");
        initialized.set(true);
      } catch (RuntimeException exception) {
        logger.error("No se pudo iniciar la conexión MQTT local", exception);
      }
      return;
    }
    intentarInicializar();
  }

  /** Retries registration every 5 seconds if fails to connect */
  @Scheduled(fixedDelayString = "${REGISTRATION_RETRY_MS:5000}")
  public void reintentarRegistro() {
    if (conectarSimulador && !initialized.get()) {
      intentarInicializar();
    }
  }

  private synchronized void intentarInicializar() {
    if (initialized.get()) {
      return;
    }
    try {
      RegistroControlador registro =
          simulatorClient
              .post()
              .uri("/controladores")
              .body(new AltaControlador(controllerLabel, controllerBaseUrl, controllerApiKey))
              .retrieve()
              .body(RegistroControlador.class);
      if (registro == null
          || registro.mqtt() == null
          || registro.mqtt().host() == null
          || registro.mqtt().host().isBlank()
          || registro.mqtt().puerto() <= 0) {
        throw new IllegalStateException("El simulador devolvió un registro sin broker MQTT válido");
      }

      String broker = "tcp://" + registro.mqtt().host() + ":" + registro.mqtt().puerto();
      mqttSubscriptionService.conectar(broker, mqttClientId);
      initialized.set(true);
      logger.info(
          "Integración con simulador lista. Etiqueta: {}, broker MQTT: {}",
          controllerLabel,
          broker);
    } catch (RuntimeException exception) {
      logger.warn(
          "No se pudo inicializar la integración con el simulador; se reintentará", exception);
    }
  }

  private record AltaControlador(String etiqueta, String baseUrl, String apiKey) {}

  private record RegistroControlador(
      String id, String etiqueta, String baseUrl, String registradoEn, Mqtt mqtt) {}

  private record Mqtt(String host, int puerto) {}
}
