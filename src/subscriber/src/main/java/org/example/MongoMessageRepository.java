package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Persiste los mensajes recibidos por MQTT en MongoDB. */
public final class MongoMessageRepository implements AutoCloseable {
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoMessageRepository(String connectionUri, String databaseName, String collectionName) {
        this.client = MongoClients.create(requireConfiguration(connectionUri, "MONGO_DB_URI"));
        this.collection = client
                .getDatabase(requireConfiguration(databaseName, "MONGO_DB_DATABASE"))
                .getCollection(requireConfiguration(collectionName, "MONGO_DB_COLLECTION"));
    }

    public void save(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        Document document = new Document("topic", topic)
                .append("rawPayload", payload)
                .append("qos", message.getQos())
                .append("retained", message.isRetained())
                .append("receivedAt", Date.from(Instant.now()));

        try {
            document.append("payload", Document.parse(payload));
        } catch (RuntimeException ignored) {
            // No todos los tópicos necesariamente publican un objeto JSON.
        }

        collection.insertOne(document);
    }

    @Override
    public void close() {
        client.close();
    }

    private static String requireConfiguration(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + variableName);
        }
        return value;
    }
}