package com.iiss.sensores.grupo_2.controlador.engine.persistence;

import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MongoTemperatureWriter {
    private final MongoCollection<Document> collection;

    @Autowired
    public MongoTemperatureWriter(MongoClient mongoClient,
                                  @Value("${MONGODB_DATABASE:sensores}") String databaseName,
                                  @Value("${MONGODB_COLLECTION_MEDICIONES:mediciones}") String collectionName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }

    public MongoTemperatureWriter(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public void saveTemperature(TemperatureMessage message) {
        Document document = new Document()
                .append("id", message.id())
                .append("idTermostat", message.idTermostat())
                .append("tC", message.tC())
                .append("tF", message.tF())
                .append("ts", message.ts());
        collection.insertOne(document);
    }
}
