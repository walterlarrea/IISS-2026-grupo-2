package org.subs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.subs.controllertemp.ControllerTempAuto;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MqttSubscriberTest {
    @Mock
    private MongoTemperatureWriter writerMock;

    @Test
    void verifyMessageTest() {//verifica que llegue y guarde el mensaje
        ControllerTempAuto controllerMock = mock(ControllerTempAuto.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        MqttSubscriber subscriber = new MqttSubscriber(writerMock, controllerMock, objectMapper,"tcp://localhost:1883", "TestClient", "test/topic");
        String mensaje = """
                {"id": 1, "tC": 24.5, "tF": 76.1, "ts": 1788006901}
                """;
        subscriber.processMessage(mensaje);
        verify(writerMock, times(1)).saveTemperature(mensaje);
    }
}
