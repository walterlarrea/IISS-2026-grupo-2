package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.api.event.UpdatedInventoryEvent;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttSubscriptionServiceTest {

    @Test
    void conectaConTopicsDeInventarioYConservaElTopicDeFallback() {
        InventoryService inventoryService = mock(InventoryService.class);
        MqttSubscriber mqttSubscriber = mock(MqttSubscriber.class);
        when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(
                habitacion("room1", "sensors/room1"),
                habitacion("room2", "sensors/room2")
        )));
        MqttSubscriptionService service = new MqttSubscriptionService("#", inventoryService, mqttSubscriber);

        service.conectar("tcp://broker:1883", "subscriber");

        verify(mqttSubscriber).connect(
                "tcp://broker:1883",
                "subscriber",
                Set.of("sensors/room1", "sensors/room2")
        );
    }

    @Test
    void actualizaTopicsAlRecibirUnInventarioActualizado() {
        InventoryService inventoryService = mock(InventoryService.class);
        MqttSubscriber mqttSubscriber = mock(MqttSubscriber.class);
        MqttSubscriptionService service = new MqttSubscriptionService("#", inventoryService, mqttSubscriber);
        service.conectar("tcp://broker:1883", "subscriber");

        when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(
                habitacion("room2", "sensors/room2"),
                habitacion("room3", "sensors/room3")
        )));
        service.alActualizarInventario(new UpdatedInventoryEvent(inventario(
                habitacion("room2", "sensors/room2"),
                habitacion("room3", "sensors/room3")
        )));

        verify(mqttSubscriber).updateSubscriptions(
                Set.of("sensors/room2", "sensors/room3")
        );
    }

    private static Inventory inventario(Inventory.Habitacion... habitaciones) {
        return new Inventory(
                "2.0",
                new Inventory.Sitio(
                        "site",
                        "Site",
                        3.7,
                        new Inventory.Tarifa(new Inventory.Franja("17:00", "23:00", Inventory.Dias.HABILES))
                ),
                List.of(habitaciones)
        );
    }

    private static Inventory.Habitacion habitacion(String id, String topic) {
        return new Inventory.Habitacion(
                id, id, 21.0, 1.0, "thermostat-" + id, topic,
                "switch-" + id, "http://switches:8081"
        );
    }
}
