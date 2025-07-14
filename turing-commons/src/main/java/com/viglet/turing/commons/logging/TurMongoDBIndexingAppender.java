package com.viglet.turing.commons.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.Arrays;

@Slf4j
@Setter
public class TurMongoDBIndexingAppender extends TurMongoDBAppenderBase {

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