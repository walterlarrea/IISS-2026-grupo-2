package org.example;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)//hace que @Mock funcione automáticamente
public class MongoTemperatureTest {

    @Test
    void saveTemperatureMongoTest() {//si se guarda una temperatura correctamente
        MongoCollection<Document> collectionMock = mock(MongoCollection.class);//para crear una colección falsa.
        MongoTemperatureWriter writer = new MongoTemperatureWriter(collectionMock);//para usar un mongoDB "falso", de prueba.
        String json = """
                {"id": 1, "tC": 24.5, "tF": 76.1, "ts": 1788006901}
                """;
        writer.saveTemperature(json);
        verify(collectionMock, times(1)).insertOne(any(Document.class));
    }

    @Test
    void invalidJsonTest() {//json inválido, no se inserta en la BD
        MongoCollection<Document> collectionMock = mock(MongoCollection.class);
        MongoTemperatureWriter writer = new MongoTemperatureWriter(collectionMock);
        String jsonInvalido = "{Invalid json}";
        assertThrows(Exception.class, () -> {writer.saveTemperature(jsonInvalido);});
        verify(collectionMock, never()).insertOne(any(Document.class));
    }

}
