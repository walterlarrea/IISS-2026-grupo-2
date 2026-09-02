package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoTemperatureWriter {
    private final MongoClient mongoClient;
    private final MongoCollection<Document> collection;
    private static final AppConfig appConfig = AppConfig.getInstance();

    public MongoTemperatureWriter(MongoCollection<Document> collection) {
        this.mongoClient = null;
        this.collection = collection;
    }

    public MongoTemperatureWriter() {
        String uri = appConfig.getValues().get(appConfig.MONGODB_URI);
        String databaseName = appConfig.getValues().get(appConfig.MONGODB_DATABASE);
        String collectionName = appConfig.getValues().get(appConfig.MONGODB_COLLECTION_MEDICIONES);
        mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        collection = database.getCollection(collectionName);
    }

    public void saveTemperature(String message) {
        Document document = Document.parse(message);
        collection.insertOne(document);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
