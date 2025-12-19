package com.viglet.turing.commons.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import lombok.Setter;
import org.bson.Document;

import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

@Slf4j
@Setter
public class TurMongoDBAppender extends TurMongoDBAppenderBase {

    public static final int MAX_LENGTH_PACKAGE_NAME = 40;

    @Override
    protected void append(ILoggingEvent event) {
        if (!enabled || collection == null) {
            return;
        }
        TurLoggingGeneral turLoggingGeneral = TurLoggingGeneral.builder()
                .level(event.getLevel().toString())
                .logger(abbreviatePackage(event.getLoggerName()))
                .message(event.getFormattedMessage())
                .date(new Date(event.getTimeStamp()))
                .stackTrace(getStackTrace(event))
                .build();
        try {
            turLoggingGeneral.setClusterNode(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
        try {
            String json = new ObjectMapper().registerModule(new JodaModule())
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                    .writeValueAsString(turLoggingGeneral);
            collection.insertOne(Document.parse(json));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static @NotNull String getStackTrace(ILoggingEvent event) {
        StringBuilder stackStraceBuilder = new StringBuilder();
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            String throwableStr = ThrowableProxyUtil.asString(throwableProxy);
            stackStraceBuilder.append(throwableStr);
            stackStraceBuilder.append(CoreConstants.LINE_SEPARATOR);
        }
        return stackStraceBuilder.toString();
    }

    private static @NotNull String abbreviatePackage(String packageName) {
        if (packageName.length() <= MAX_LENGTH_PACKAGE_NAME) return packageName;
        StringBuilder stringBuilder = new StringBuilder(packageName);
        TurNameAbbreviator.getAbbreviator("1.").abbreviate(1, stringBuilder);
        return stringBuilder.toString();
    }


}