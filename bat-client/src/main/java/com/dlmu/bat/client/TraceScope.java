package com.dlmu.bat.client;

import com.google.common.collect.ImmutableMap;

import java.io.Closeable;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public interface TraceScope extends Closeable {

    Span getSpan();

    Span getParentSpan();

    void setParentSpan(Span parentSpan);

    void addKVAnnotation(String key, String value);

    void addTimelineAnnotation(String msg);

    void addTraceContext(String key, String value);

    ImmutableMap<String, String> getTraceContext();

    void detach();

    void reattach();

    void close();

}
