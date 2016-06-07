package com.dlmu.bat.client;

import com.dlmu.bat.client.receiver.logger.DefaultAsyncLogger;
import com.dlmu.bat.client.receiver.logger.LogEvent;
import com.dlmu.bat.client.receiver.logger.LogEventConsumer;
import com.lmax.disruptor.EventTranslatorOneArg;
import org.junit.Test;

/**
 * @author heipacker
 * @date 16-5-29.
 */
public class AsyncLoggerTest {

    @Test
    public void testAsyncLogger() throws InterruptedException {
        LogEventConsumer logEventConsumer = new LogEventConsumer("/tmp/test.log");
        DefaultAsyncLogger asyncLogger = new DefaultAsyncLogger(128, logEventConsumer, new EventTranslatorOneArg<LogEvent, LogEvent>() {
            /**
             * Translate a data representation into fields set in given event
             *
             * @param event    into which the data should be translated.
             * @param sequence that is assigned to event.
             * @param arg0     The first user specified argument to the translator
             */
            @Override
            public void translateTo(LogEvent event, long sequence, LogEvent arg0) {
                event.setLog(arg0.getLog());
            }
        });


        for (int i = 0; i < 10000; ++i) {
            asyncLogger.write(new LogEvent("teset" + i));
        }
        Thread.sleep(1000);
        logEventConsumer.close();
    }
}
