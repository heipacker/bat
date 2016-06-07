package com.dlmu.bat.client.receiver.logger;

import com.lmax.disruptor.EventFactory;

/**
 * @author heipacker
 * @date 16-5-29.
 */
public class LogEvent {

    private String log;

    public LogEvent() {

    }

    public LogEvent(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public static EventFactory<LogEvent> getEventFactory() {
        return new EventFactory<LogEvent>() {
            public LogEvent newInstance() {
                return new LogEvent();
            }
        };
    }
}
