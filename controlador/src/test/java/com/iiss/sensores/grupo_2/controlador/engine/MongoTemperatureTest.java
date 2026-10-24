package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MongoTemperatureTest {

    @Test
    void saveTemperatureMongoTest() {
        MongoCollection<Document> collectionMock = mock(MongoCollection.class);
        TemperatureReadingRepository writer = new TemperatureReadingRepository(collectionMock);

        TemperatureMessage message = new TemperatureMessage(
                "1",
                "ht-home1",
                24.5,
                76.1,
                Instant.ofEpochMilli(1788006901000L)
        );
        writer.saveTemperature(message);

        verify(collectionMock, times(1)).insertOne(any(Document.class));
    }
}
