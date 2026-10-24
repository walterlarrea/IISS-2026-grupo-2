package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.api.event.InventarioActualizadoEvent;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
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
        InventarioService inventarioService = mock(InventarioService.class);
        MqttSubscriber mqttSubscriber = mock(MqttSubscriber.class);
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(
                habitacion("room1", "sensors/room1"),
                habitacion("room2", "sensors/room2")
        )));
        MqttSubscriptionService service = new MqttSubscriptionService("#", inventarioService, mqttSubscriber);

        service.conectar("tcp://broker:1883", "subscriber");

        verify(mqttSubscriber).connect(
                "tcp://broker:1883",
                "subscriber",
                Set.of("sensors/room1", "sensors/room2")
        );
    }

    @Test
    void actualizaTopicsAlRecibirUnInventarioActualizado() {
        InventarioService inventarioService = mock(InventarioService.class);
        MqttSubscriber mqttSubscriber = mock(MqttSubscriber.class);
        MqttSubscriptionService service = new MqttSubscriptionService("#", inventarioService, mqttSubscriber);
        service.conectar("tcp://broker:1883", "subscriber");

        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(
                habitacion("room2", "sensors/room2"),
                habitacion("room3", "sensors/room3")
        )));
        service.alActualizarInventario(new InventarioActualizadoEvent(inventario(
                habitacion("room2", "sensors/room2"),
                habitacion("room3", "sensors/room3")
        )));

        verify(mqttSubscriber).updateSubscriptions(
                Set.of("sensors/room2", "sensors/room3")
        );
    }

    private static Inventario inventario(Inventario.Habitacion... habitaciones) {
        return new Inventario(
                "2.0",
                new Inventario.Sitio(
                        "site",
                        "Site",
                        3.7,
                        new Inventario.Tarifa(new Inventario.Franja("17:00", "23:00", Inventario.Dias.HABILES))
                ),
                List.of(habitaciones)
        );
    }

    private static Inventario.Habitacion habitacion(String id, String topic) {
        return new Inventario.Habitacion(
                id, id, 21.0, 1.0, "thermostat-" + id, topic,
                "switch-" + id, "http://switches:8081"
        );
    }
}
