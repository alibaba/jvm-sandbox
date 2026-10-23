package com.alibaba.jvm.sandbox.core.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LogbackUtilsTest {

    @Test
    public void logsFailureWhenLoggerContextCannotStop() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LogbackUtils.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>();
        final boolean additive = logger.isAdditive();
        appender.start();
        logger.setAdditive(false);
        logger.addAppender(appender);
        try {
            final LoggerContext loggerContext = new LoggerContext() {
                @Override
                public void stop() {
                    throw new IllegalStateException("synthetic stop failure");
                }
            };

            LogbackUtils.destroy(loggerContext);

            assertEquals(1, appender.list.size());
            final ILoggingEvent event = appender.list.get(0);
            assertEquals(Level.WARN, event.getLevel());
            assertEquals("destroy logback failed.", event.getFormattedMessage());
            assertNotNull(event.getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
            logger.setAdditive(additive);
            appender.stop();
        }
    }
}
