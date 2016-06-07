package com.dlmu.bat.client.receiver.logger;

/**
 * @author heipacker
 * @date 16-5-29.
 */
public interface AsyncLogger {

    void write(LogEvent logEvent);

    void write(LogEvent logEvent, boolean end);

    void close();
}
