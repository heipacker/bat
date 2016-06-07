package com.dlmu.bat.client;

import com.google.common.collect.ImmutableMap;

import java.io.Closeable;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public interface TraceScope extends Closeable {

    /**
     * get current span of the traceScope
     *
     * @return Span
     */
    Span getSpan();

    /**
     * get current parent span of the traceScope
     *
     * @return Span
     */
    Span getParentSpan();

    /**
     * set parent span to traceScope
     *
     * @param parentSpan Span
     */
    void setParentSpan(Span parentSpan);

    /**
     * add key/value to current span
     *
     * @param key   String key
     * @param value String value
     */
    void addKVAnnotation(String key, String value);

    /**
     * add timed event to current span
     *
     * @param msg String event message
     */
    void addTimelineAnnotation(String msg);

    /**
     * add trace context to current span
     *
     * @param key   String key
     * @param value String value
     */
    void addTraceContext(String key, String value);

    /**
     * get current trace context
     *
     * @return current trace context
     */
    ImmutableMap<String, String> getTraceContext();

    /**
     * detach current span from the thread local
     */
    void detach();

    /**
     * reattach current span to the thread local
     */
    void reattach();

    /**
     * close current span
     */
    void close();

}
