package com.viglet.turing.commons.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class TurMongoDBAppenderBaseTest {

    @Test
    void shouldStartAndStopSafelyEvenWithInvalidConfiguration() {
        TestAppender appender = new TestAppender();
        appender.setEnabled(true);
        appender.setConnectionString("mongodb://invalid-host:27017");
        appender.setDatabaseName("testdb");
        appender.setCollectionName("logs");

        assertThatCode(appender::start).doesNotThrowAnyException();
        assertThatCode(appender::stop).doesNotThrowAnyException();
    }

    private static class TestAppender extends TurMongoDBAppenderBase {
        @Override
        protected void append(ILoggingEvent iLoggingEvent) {
        }
    }
}
