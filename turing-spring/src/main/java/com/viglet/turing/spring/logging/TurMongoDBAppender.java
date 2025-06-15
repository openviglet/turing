package com.viglet.turing.spring.logging;

import com.mongodb.client.MongoClient;
import lombok.Setter;
import org.bson.Document;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class TurMongoDBAppender extends AppenderBase<ILoggingEvent> {
    private boolean enabled;
    private String connectionString;
    private String databaseName;
    private String collectionName;
    private MongoCollection<org.bson.Document> collection;

    @Override
    public void start() {
        super.start();
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            collection = database.getCollection(collectionName);
        } catch (Exception e) {
            addError("Error connecting to MongoDB", e);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!enabled || collection == null) {
            return;
        }
        Document log = new Document("level", eventObject.getLevel().toString())
                .append("logger", eventObject.getLoggerName())
                .append("message", eventObject.getFormattedMessage())
                .append("timestamp", eventObject.getTimeStamp());
        collection.insertOne(log);
    }


}