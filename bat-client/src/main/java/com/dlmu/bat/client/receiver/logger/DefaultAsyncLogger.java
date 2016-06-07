package com.dlmu.bat.client.receiver.logger;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author heipacker
 * @date 16-5-29.
 */
public class DefaultAsyncLogger implements AsyncLogger {

    private Disruptor<LogEvent> disruptor;

    private RingBuffer<LogEvent> ringBuffer;

    private EventHandlerGroup<LogEvent> eventHandlerGroup;

    private EventTranslatorOneArg<LogEvent, LogEvent> translatorOneArg;

    private EventHandler<LogEvent>[] eventEventHandlers;

    public DefaultAsyncLogger(int ringSize, EventHandler<LogEvent> eventHandler, EventTranslatorOneArg<LogEvent, LogEvent> translatorOneArg) {
        this(ringSize, (EventHandler<LogEvent>[]) new EventHandler<?>[]{eventHandler}, translatorOneArg);
    }

    public DefaultAsyncLogger(int ringSize, EventHandler<LogEvent>[] eventHandlers, EventTranslatorOneArg<LogEvent, LogEvent> translatorOneArg) {
        this.disruptor = new Disruptor<LogEvent>(LogEvent.getEventFactory(),
                ringSize,
                new ThreadFactory() {
                    private final AtomicInteger index = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread();
                        thread.setDaemon(true);
                        thread.setName("AsyncLoggerThread" + index.getAndIncrement());
                        return thread;
                    }
                },
                ProducerType.MULTI,
                new SleepingWaitStrategy());
        this.eventEventHandlers = eventHandlers;
        this.eventHandlerGroup = disruptor.handleEventsWith(eventHandlers);
        this.translatorOneArg = translatorOneArg;
        this.ringBuffer = disruptor.start();
    }

    public void write(final LogEvent logEvent) {
        ringBuffer.publishEvent(translatorOneArg, logEvent);
    }

    public void write(final LogEvent logEvent, boolean end) {
        ringBuffer.publishEvent(translatorOneArg, logEvent);
    }

    @Override
    public void close() {
        disruptor.shutdown();
    }
}
