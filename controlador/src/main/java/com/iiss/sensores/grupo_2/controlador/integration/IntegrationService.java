package com.iiss.sensores.grupo_2.controlador.integration;

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

import java.util.concurrent.atomic.AtomicBoolean;

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
    private final String fallbackTopic;
    private final RestClient simulatorClient;
    private final InventarioService inventarioService;
    private final MqttSubscriber mqttSubscriber;
    private final AtomicBoolean initialized = new AtomicBoolean();

    public IntegrationService(
            RestClient.Builder restClientBuilder,
            InventarioService inventarioService,
            MqttSubscriber mqttSubscriber,
            @Value("${CONECTAR_CON_SIMULADOR:false}") boolean conectarSimulador,
            @Value("${SIMULATOR_API_URL:}") String simulatorApiUrl,
            @Value("${SIMULATOR_API_KEY:}") String simulatorApiKey,
            @Value("${CONTROLADOR_ETIQUETA:controlador}") String controllerLabel,
            @Value("${CONTROLADOR_API_URL:http://localhost:8080}") String controllerBaseUrl,
            @Value("${CONTROLADOR_API_KEY:development-key}") String controllerApiKey,
            @Value("${MQTT_BROKER_URL:tcp://localhost:1883}") String fallbackBroker,
            @Value("${MQTT_CLIENT_ID:ControladorSubscriber}") String mqttClientId,
            @Value("${MQTT_TOPIC:sensores/temperatura}") String fallbackTopic
    ) {
        this.conectarSimulador = conectarSimulador;
        this.simulatorApiUrl = simulatorApiUrl;
        this.simulatorApiKey = simulatorApiKey;
        this.controllerLabel = controllerLabel;
        this.controllerBaseUrl = controllerBaseUrl;
        this.controllerApiKey = controllerApiKey;
        this.fallbackBroker = fallbackBroker;
        this.mqttClientId = mqttClientId;
        this.fallbackTopic = fallbackTopic;

        RestClient.Builder clientBuilder = restClientBuilder.clone();
        if(conectarSimulador) {
            logger.info("Integración con simulador habilitada; se intentará registrar el controlador en {}", simulatorApiUrl);
            if (!simulatorApiUrl.isBlank()) {
                clientBuilder.baseUrl(simulatorApiUrl);
            }
            if (!simulatorApiKey.isBlank()) {
                clientBuilder.defaultHeader("X-API-Key", simulatorApiKey);
            }
        } else {
            logger.info("Integración con simulador deshabilitada; se usará broker y topic MQTT propios: {} y {}", fallbackBroker, fallbackTopic);
        }

        this.simulatorClient = clientBuilder.build();
        this.inventarioService = inventarioService;
        this.mqttSubscriber = mqttSubscriber;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!conectarSimulador) {
            try {
                mqttSubscriber.conectar(fallbackBroker, mqttClientId, fallbackTopic);
                logger.info("Integración con simulador deshabilitada; usando broker y topic configurados localmente");
                initialized.set(true);
            } catch (RuntimeException exception) {
                logger.error("No se pudo iniciar la conexión MQTT local", exception);
            }
            return;
        }
        intentarInicializar();
    }

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

            // Inventario inventario = simulatorClient.get()
            //         .uri("/sitio")
            //         .retrieve()
            //         .body(Inventario.class);
            // if (inventario == null) {
            //     throw new IllegalStateException("El simulador devolvió un inventario vacío");
            // }
            // inventarioService.reemplazar(inventario);

            // Map<String, String> termostatosPorTopic = new LinkedHashMap<>();
            // for (Inventario.Habitacion habitacion : inventario.habitaciones()) {
            //     String anterior = termostatosPorTopic.putIfAbsent(
            //             habitacion.topicTermostato(),
            //             habitacion.idTermostato()
            //     );
            //     if (anterior != null && !anterior.equals(habitacion.idTermostato())) {
            //         throw new IllegalStateException(
            //                 "El topic MQTT " + habitacion.topicTermostato()
            //                         + " está asociado a más de un termostato"
            //         );
            //     }
            // }
            String broker = "tcp://" + registro.mqtt().host() + ":" + registro.mqtt().puerto();
            mqttSubscriber.conectar(broker, mqttClientId, fallbackTopic);
            initialized.set(true);
            logger.info(
                    "Integración con simulador lista. Etiqueta: {}, broker MQTT: {}, topics: {}",
                    controllerLabel,
                    broker
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
