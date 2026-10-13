package com.iiss.sensores.grupo_2.controlador.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.MongoTemperatureWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MqttSubscriberTest {
    @Mock
    private MongoTemperatureWriter writerMock;

    @Test
    void verifyMessageTest() {
        ControllerTempAuto controllerMock = mock(ControllerTempAuto.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        MqttSubscriber subscriber = new MqttSubscriber(writerMock, controllerMock, objectMapper);
        String mensaje = """
                {"id": "termo1", "tC": 24.5, "tF": 76.1, "ts": 1788006901}
                """;
        subscriber.processMessage(mensaje);
        verify(writerMock, times(1)).saveTemperature(mensaje);
    }

    @Test
    void resolvesThermostatFromInventoryTopicInsteadOfNumericSensorId() {
        assertEquals(
                "ht-room1",
                MqttSubscriber.resolverIdTermostato(
                        "ht-room1/status/temperature:0",
                        "0",
                        Map.of("ht-room1/status/temperature:0", "ht-room1")
                )
        );
    }

    @Test
    void usesMessageIdWhenTopicHasNoInventoryMapping() {
        assertEquals(
                "termo-local",
                MqttSubscriber.resolverIdTermostato("sensores/temperatura", "termo-local", Map.of())
        );
    }
}
