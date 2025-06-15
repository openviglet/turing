package com.viglet.turing.spring.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Setter
public class TurMongoDBAppenderBase extends AppenderBase<ILoggingEvent> {
    protected boolean enabled;
    protected String connectionString;
    protected String databaseName;
    protected String collectionName;
    protected MongoCollection<Document> collection;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        //
    }

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
}