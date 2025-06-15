package com.viglet.turing.api.logging;

import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/logging")
@Tag(name = "Logging", description = "Logging API")
public class TurLoggingAPI {
    public static final String TIMESTAMP = "timestamp";
    private final boolean enabled;
    private final String connectionString;
    private final String databaseName;
    private final String generalCollectionName;
    private final String indexingCollectionName;

    public TurLoggingAPI(@Value("${turing.mongodb.enabled:false}") boolean enabled,
                         @Value("${turing.mongodb.uri:''}") String connectionString,
                         @Value("${turing.mongodb.logging.database:''}")String databaseName,
                         @Value("${turing.mongodb.logging.collection.general:''}")String generalCollectionName,
                         @Value("${turing.mongodb.logging.collection.indexing:''}") String indexingCollectionName) {
        this.enabled = enabled;
        this.connectionString = connectionString;
        this.databaseName = databaseName;
        this.generalCollectionName = generalCollectionName;
        this.indexingCollectionName = indexingCollectionName;
    }

    @Operation(summary = "General Logging")
    @GetMapping
    public List<Document> generalLogging() {
        List<Document> documentList = new ArrayList<>();
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> generalCollection = database.getCollection(generalCollectionName);
             generalCollection.find()
                    .sort(Sorts.descending(TIMESTAMP))
                    .limit(50).forEach(documentList::add);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
        return documentList;
    }

    @Operation(summary = "General Logging")
    @GetMapping("indexing")
    public List<Document> indexingLogging() {
        List<Document> documentList = new ArrayList<>();
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> indexingCollection = database.getCollection(indexingCollectionName);
            indexingCollection.find()
                    .sort(Sorts.descending(TIMESTAMP))
                    .limit(50).forEach(documentList::add);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return documentList;
    }



}
