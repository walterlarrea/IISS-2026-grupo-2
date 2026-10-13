package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SimulatorIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(SimulatorIntegrationService.class);

    private final String simulatorApiUrl;
    private final String simulatorApiKey;
    private final String controllerLabel;
    private final String controllerBaseUrl;
    private final String controllerApiKey;
    private final String fallbackBroker;
    private final String mqttClientId;
    private final String fallbackTopic;
    private final RestClient simulatorClient;
    private final InventarioService inventarioService;
    private final MqttSubscriber mqttSubscriber;
    private final AtomicBoolean initialized = new AtomicBoolean();

    public SimulatorIntegrationService(
            RestClient.Builder restClientBuilder,
            InventarioService inventarioService,
            MqttSubscriber mqttSubscriber,
            @Value("${SIMULATOR_API_URL:}") String simulatorApiUrl,
            @Value("${SIMULATOR_API_KEY:}") String simulatorApiKey,
            @Value("${CONTROLADOR_ETIQUETA:controlador}") String controllerLabel,
            @Value("${CONTROLADOR_BASE_URL:http://localhost:8080}") String controllerBaseUrl,
            @Value("${ROOMS_API_KEY:development-key}") String controllerApiKey,
            @Value("${MQTT_BROKER_URL:tcp://localhost:1883}") String fallbackBroker,
            @Value("${MQTT_CLIENT_ID:ControladorSubscriber}") String mqttClientId,
            @Value("${MQTT_TOPIC:sensores/temperatura}") String fallbackTopic
    ) {
        this.simulatorApiUrl = simulatorApiUrl;
        this.simulatorApiKey = simulatorApiKey;
        this.controllerLabel = controllerLabel;
        this.controllerBaseUrl = controllerBaseUrl;
        this.controllerApiKey = controllerApiKey;
        this.fallbackBroker = fallbackBroker;
        this.mqttClientId = mqttClientId;
        this.fallbackTopic = fallbackTopic;

        RestClient.Builder clientBuilder = restClientBuilder.clone();
        if (!simulatorApiUrl.isBlank()) {
            clientBuilder.baseUrl(simulatorApiUrl);
        }
        if (!simulatorApiKey.isBlank()) {
            clientBuilder.defaultHeader("X-API-Key", simulatorApiKey);
        }
        this.simulatorClient = clientBuilder.build();
        this.inventarioService = inventarioService;
        this.mqttSubscriber = mqttSubscriber;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (simulatorApiUrl.isBlank()) {
            try {
                mqttSubscriber.conectar(fallbackBroker, mqttClientId, Map.of(fallbackTopic, ""));
                logger.info("Integración con simulador deshabilitada; usando broker y topic configurados localmente");
                initialized.set(true);
            } catch (RuntimeException exception) {
                logger.error("No se pudo iniciar la conexión MQTT local", exception);
            }
            return;
        }
        intentarInicializar();
    }

    @Scheduled(fixedDelayString = "${SIMULATOR_REGISTRATION_RETRY_MS:5000}")
    public void reintentarRegistro() {
        if (!simulatorApiUrl.isBlank() && !initialized.get()) {
            intentarInicializar();
        }
    }

    private synchronized void intentarInicializar() {
        if (initialized.get()) {
            return;
        }
        try {
            RegistroControlador registro = simulatorClient.post()
                    .uri("/controladores")
                    .body(new AltaControlador(controllerLabel, controllerBaseUrl, controllerApiKey))
                    .retrieve()
                    .body(RegistroControlador.class);
            if (registro == null || registro.mqtt() == null
                    || registro.mqtt().host() == null || registro.mqtt().host().isBlank()
                    || registro.mqtt().puerto() <= 0) {
                throw new IllegalStateException("El simulador devolvió un registro sin broker MQTT válido");
            }

            Inventario inventario = simulatorClient.get()
                    .uri("/sitio")
                    .retrieve()
                    .body(Inventario.class);
            if (inventario == null) {
                throw new IllegalStateException("El simulador devolvió un inventario vacío");
            }

            Map<String, String> termostatosPorTopic = new LinkedHashMap<>();
            for (Inventario.Habitacion habitacion : inventario.habitaciones()) {
                String anterior = termostatosPorTopic.putIfAbsent(
                        habitacion.topicTermostato(),
                        habitacion.idTermostato()
                );
                if (anterior != null && !anterior.equals(habitacion.idTermostato())) {
                    throw new IllegalStateException("El topic MQTT " + habitacion.topicTermostato() + " está asociado a más de un termostato");
                }
            }
            inventarioService.reemplazar(inventario);

            String broker = "tcp://" + registro.mqtt().host() + ":" + registro.mqtt().puerto();
            mqttSubscriber.conectar(broker, mqttClientId, termostatosPorTopic);
            initialized.set(true);
            logger.info(
                    "Integración con simulador lista. Etiqueta: {}, broker MQTT: {}, topics: {}",
                    controllerLabel,
                    broker,
                    termostatosPorTopic.size()
            );
        } catch (RuntimeException exception) {
            logger.warn("No se pudo inicializar la integración con el simulador; se reintentará", exception);
        }
    }

    private record AltaControlador(String etiqueta, String baseUrl, String apiKey) {
    }

    private record RegistroControlador(String id, String etiqueta, String baseUrl, String registradoEn, Mqtt mqtt) {
    }

    private record Mqtt(String host, int puerto) {
    }
}
