package com.viglet.turing.api.logging;

import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
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
    private final String serverCollectionName;
    private final String indexingCollectionName;
    private final String aemCollectionName;

    public TurLoggingAPI(@Value("${turing.mongodb.enabled:false}") boolean enabled,
                         @Value("${turing.mongodb.uri:'mongodb://localhost:27017'}") String connectionString,
                         @Value("${turing.mongodb.logging.database:'turingLog'}") String databaseName,
                         @Value("${turing.mongodb.logging.collection.server:'server'}") String serverCollectionName,
                         @Value("${turing.mongodb.logging.collection.indexing:'indexing'}") String indexingCollectionName,
                         @Value("${turing.mongodb.logging.collection.aem:'aem'}") String aemCollectionName) {
        this.enabled = enabled;
        this.connectionString = connectionString;
        this.databaseName = databaseName;
        this.serverCollectionName = serverCollectionName;
        this.indexingCollectionName = indexingCollectionName;
        this.aemCollectionName = aemCollectionName;
    }

    @Operation(summary = "Server Logging")
    @GetMapping
    public List<Document> serverLogging() {
        return getDocuments(serverCollectionName);
    }

    @Operation(summary = "Indexing Logging")
    @GetMapping("indexing")
    public List<Document> indexingLogging() {
        return getDocuments(indexingCollectionName);
    }

    @Operation(summary = "AEM Logging")
    @GetMapping("aem")
    public List<Document> aemLogging() {
        return getDocuments(aemCollectionName);
    }

    private @NotNull List<Document> getDocuments(String ii) {
        List<Document> documentList = new ArrayList<>();
        if (!enabled) {
            return documentList;
        }
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> indexingCollection = database.getCollection(ii);
            indexingCollection.find()
                    .sort(Sorts.descending(TIMESTAMP))
                    .limit(100).forEach(documentList::add);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        Collections.reverse(documentList);
        return documentList;
    }


}
