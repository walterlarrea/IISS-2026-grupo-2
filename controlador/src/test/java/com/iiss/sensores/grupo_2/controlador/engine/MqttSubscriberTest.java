package com.iiss.sensores.grupo_2.controlador.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.MongoTemperatureWriter;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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

        TemperatureMessage message = new TemperatureMessage(
                "1",
                "termo1",
                24.5,
                76.1,
                Instant.ofEpochMilli(1788006901000L)
        );

        subscriber.processMessage(message);
        verify(writerMock, times(1)).saveTemperature(message);
    }
}
