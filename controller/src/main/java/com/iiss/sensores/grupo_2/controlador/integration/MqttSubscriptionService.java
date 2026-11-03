package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.api.event.UpdatedInventoryEvent;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Gestiona las suscripciones MQTT según los topics del inventario activo. */
@Service
public class MqttSubscriptionService {
  private static final Logger logger = LoggerFactory.getLogger(MqttSubscriptionService.class);

  private final InventoryService inventoryService;
  private final MqttSubscriber mqttSubscriber;
  private final String fallbackTopic;

  /** Crea el servicio con el topic de respaldo, inventario y suscriptor MQTT. */
  public MqttSubscriptionService(
      @Value("${MQTT_TOPIC:sensores/temperatura}") String fallbackTopic,
      InventoryService inventoryService,
      MqttSubscriber mqttSubscriber) {
    this.inventoryService = inventoryService;
    this.mqttSubscriber = mqttSubscriber;
    this.fallbackTopic = fallbackTopic;
  }

  /** Conecta al broker y se suscribe a los topics configurados. */
  public void conectar(String broker, String clientId) {
    mqttSubscriber.connect(broker, clientId, obtenerTopicsDeseados());
  }

  /** Sincroniza las suscripciones cuando cambia el inventario. */
  @EventListener
  public void alActualizarInventario(UpdatedInventoryEvent event) {
    try {
      mqttSubscriber.updateSubscriptions(obtenerTopicsDeseados());
    } catch (RuntimeException exception) {
      logger.error(
          "No se pudieron sincronizar los topics MQTT del Inventory actualizado", exception);
    }
  }

  /** Obtiene los topics de habitaciones o el topic de respaldo si no hay habitaciones. */
  private Set<String> obtenerTopicsDeseados() {
    Set<String> topics =
        inventoryService.obtenerActivo().map(Inventory::habitaciones).stream()
            .flatMap(java.util.Collection::stream)
            .map(Inventory.Habitacion::topicTermostato)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    // Agregar el topic de fallback solo si la lista de topics deseados está vacía, para evitar
    // suscribirse a un topic innecesario.
    if (topics.isEmpty() && fallbackTopic != null) {
      topics.add(fallbackTopic);
    }
    return topics;
  }
}
