package org.iiss.grupo_2.sub;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.iiss.grupo_2.sub.service.MongoTemperatureWriter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MongoTemperatureTest {

    @Test
    void saveTemperatureMongoTest() {
        MongoCollection<Document> collectionMock = mock(MongoCollection.class);
        MongoTemperatureWriter writer = new MongoTemperatureWriter(collectionMock);
        String json = """
                {"id": 1, "tC": 24.5, "tF": 76.1, "ts": 1788006901}
                """;
        writer.saveTemperature(json);
        verify(collectionMock, times(1)).insertOne(any(Document.class));
    }

    @Test
    void invalidJsonTest() {
        MongoCollection<Document> collectionMock = mock(MongoCollection.class);
        MongoTemperatureWriter writer = new MongoTemperatureWriter(collectionMock);
        String jsonInvalido = "{Invalid json}";
        assertThrows(Exception.class, () -> {
            writer.saveTemperature(jsonInvalido);
        });
        verify(collectionMock, never()).insertOne(any(Document.class));
    }
}
