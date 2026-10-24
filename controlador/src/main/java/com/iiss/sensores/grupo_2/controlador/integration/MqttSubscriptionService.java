package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.api.event.InventarioActualizadoEvent;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MqttSubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriptionService.class);

    private final InventarioService inventarioService;
    private final MqttSubscriber mqttSubscriber;
    private final String fallbackTopic;

    public MqttSubscriptionService(
            @Value("${MQTT_TOPIC:sensores/temperatura}") String fallbackTopic,
            InventarioService inventarioService,
            MqttSubscriber mqttSubscriber
        ) {
        this.inventarioService = inventarioService;
        this.mqttSubscriber = mqttSubscriber;
        this.fallbackTopic = fallbackTopic;
    }

    public synchronized void conectar(String broker, String clientId) {
        mqttSubscriber.connect(broker, clientId, obtenerTopicsDeseados());
    }

    @EventListener
    public synchronized void alActualizarInventario(InventarioActualizadoEvent event) {
        try {
            mqttSubscriber.updateSubscriptions(obtenerTopicsDeseados());
        } catch (RuntimeException exception) {
            logger.error("No se pudieron sincronizar los topics MQTT del Inventario actualizado", exception);
        }
    }

    private Set<String> obtenerTopicsDeseados() {
        Set<String> topics = inventarioService.obtenerActivo()
                .map(Inventario::habitaciones)
                .stream()
                .flatMap(java.util.Collection::stream)
                .map(Inventario.Habitacion::topicTermostato)
                .collect(Collectors.toCollection(LinkedHashSet::new));
                // Agregar el topic de fallback solo si la lista de topics deseados está vacía, para evitar suscribirse a un topic innecesario.
        if (topics.isEmpty() && fallbackTopic != null) {
            topics.add(fallbackTopic);
        }
        return topics;
    }
}
