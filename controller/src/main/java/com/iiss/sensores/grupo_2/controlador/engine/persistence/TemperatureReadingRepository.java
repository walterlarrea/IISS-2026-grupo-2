package com.iiss.sensores.grupo_2.controlador.engine.persistence;

import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TemperatureReadingRepository {
    private static final Logger logger = LoggerFactory.getLogger(TemperatureReadingRepository.class);
    private final MongoCollection<Document> collection;

    @Autowired
    public TemperatureReadingRepository(MongoClient mongoClient,
                                        @Value("${MONGODB_DATABASE:sensores}") String databaseName,
                                        @Value("${MONGODB_COLLECTION_MEDICIONES:mediciones}") String collectionName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
        this.createIndexes();
    }

    public TemperatureReadingRepository(MongoCollection<Document> collection) {
        this.collection = collection;
        this.createIndexes();
    }

    private void createIndexes() {
        // Create an index on the "idThermostat" field for efficient querying
        try {
            collection.createIndex(new Document("idThermostat", 1).append("ts", -1));
        } catch (RuntimeException e) {
            logger.debug("Error creating indexes on " + this.collection.getNamespace() + " collection");
        }
    }

    public void saveTemperature(TemperatureMessage message) {
        Document document = new Document()
                .append("id", message.id())
                .append("idThermostat", message.idThermostat())
                .append("tC", message.tC())
                .append("tF", message.tF())
                .append("ts", message.ts());
        collection.insertOne(document);
    }

    public List<TemperatureMessage> getLastTemperatureMeasurements(List<String> idThermostats) {
        // Implement the logic to retrieve the last temperature measurements associated to each thermostat ID in the list.
        List<Bson> pipeline = List.of(
                Aggregates.match(
                        Filters.in("idThermostat", idThermostats)
                ),
                Aggregates.sort(
                        Sorts.descending("ts")
                ),
                Aggregates.group(
                        "$idThermostat",
                        Accumulators.first("lastRecord", "$$ROOT")
                ),
                Aggregates.replaceRoot("$lastRecord")
        );

        return this.collection.aggregate(pipeline, TemperatureMessage.class)
                .into(new ArrayList<>());
    }
}
