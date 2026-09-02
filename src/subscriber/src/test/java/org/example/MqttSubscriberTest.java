package org.example;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

public class MqttSubscriberTest {

    @Test
    void verifyMessageTest() {//verifica que llegue y guarde el mensaje
        MongoTemperatureWriter writerMock = mock(MongoTemperatureWriter.class);
        MqttSubscriber subscriber = new MqttSubscriber(writerMock);
        String mensaje = """
                {"id": 1, "tC": 24.5, "tF": 76.1, "ts": 1788006901}
                """;
        subscriber.processMessage(mensaje);
        verify(writerMock, times(1)).saveTemperature(mensaje);
    }
}
