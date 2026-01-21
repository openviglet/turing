package com.viglet.turing.commons.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.joda.JodaModule;

@Slf4j
@Setter
public class TurMongoDBAppender extends TurMongoDBAppenderBase {

    public static final int MAX_LENGTH_PACKAGE_NAME = 40;
    private static final Abbreviator ABBREVIATOR = new TargetLengthBasedClassNameAbbreviator(1);

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
            // Use um fallback ou log de console aqui, evite recursão se o log falhar
            System.err.println(e.getMessage());
        }
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new JodaModule())
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                .build();
        String json = mapper.writeValueAsString(turLoggingGeneral);
        collection.insertOne(Document.parse(json));

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
        if (packageName == null)
            return "";
        if (packageName.length() <= MAX_LENGTH_PACKAGE_NAME)
            return packageName;
        return ABBREVIATOR.abbreviate(packageName);
    }
}