package com.viglet.turing.spring.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.bson.Document;

import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
@Slf4j
@Setter
public class TurMongoDBAppender extends TurMongoDBAppenderBase {
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!enabled || collection == null) {
            return;
        }
        TurLoggingGeneral turLoggingGeneral = TurLoggingGeneral.builder()
                .level(eventObject.getLevel().toString())
                .logger(abbreviatePackage(eventObject.getLoggerName()))
                .message(eventObject.getFormattedMessage())
                .timestamp(new Date(eventObject.getTimeStamp()))
                .build();
        try {
            collection.insertOne(Document.parse(new ObjectMapper().writeValueAsString(turLoggingGeneral)));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static @NotNull String abbreviatePackage(String packageName) {
        NameAbbreviator n = NameAbbreviator.getAbbreviator("1.");
        StringBuffer sb = new StringBuffer();
        sb.append(packageName);
        n.abbreviate(1, sb);
        return sb.toString();
    }


}