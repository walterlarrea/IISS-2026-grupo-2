package org.subs;

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

    //constructor para springboot
    @Autowired
    public MongoTemperatureWriter(MongoClient mongoClient, @Value("${MONGODB_DATABASE}") String databaseName, @Value("${MONGODB_COLLECTION_MEDICIONES}") String collectionName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }

    //constructor para los tests
    public MongoTemperatureWriter(MongoCollection<Document> collection) {this.collection = collection;}

    public void saveTemperature(String message) {
        Document document = Document.parse(message);
        collection.insertOne(document);
    }
}
