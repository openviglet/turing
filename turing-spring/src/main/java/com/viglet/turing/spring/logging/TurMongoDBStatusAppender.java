package com.viglet.turing.spring.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.Arrays;

@Slf4j
@Setter
public class TurMongoDBStatusAppender extends AppenderBase<ILoggingEvent> {
    private boolean enabled;
    private String connectionString;
    private String databaseName;
    private String collectionName;
    private MongoCollection<Document> collection;

    @Override
    public void start() {
        super.start();
        try {
            var mongoClient = MongoClients.create(connectionString);
            collection = mongoClient
                    .getDatabase(databaseName)
                    .getCollection(collectionName);
        } catch (Exception e) {
            addError("Error connecting to MongoDB", e);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!enabled || collection == null) {
            return;
        }
        Arrays.stream(eventObject.getArgumentArray()).forEach(object -> {
            try {
                collection.insertOne(Document.parse(new ObjectMapper().writeValueAsString(object)));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
            }

        });
    }
}