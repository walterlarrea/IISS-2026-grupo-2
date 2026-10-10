package com.iiss.sensores.grupo_2.controlador.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.writer.MongoTemperatureWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MqttSubscriberTest {
    @Mock
    private MongoTemperatureWriter writerMock;

    @Test
    void verifyMessageTest() {
        ControllerTempAuto controllerMock = mock(ControllerTempAuto.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        MqttSubscriber subscriber = new MqttSubscriber(writerMock, controllerMock, objectMapper, "tcp://localhost:1883", "TestClient", "test/topic");
        String mensaje = """
                {"id": 1, "tC": 24.5, "tF": 76.1, "ts": 1788006901}
                """;
        subscriber.processMessage(mensaje);
        verify(writerMock, times(1)).saveTemperature(mensaje);
    }
}
