package com.dlmu.bat.client;

import com.google.common.collect.ImmutableMap;

import java.io.Closeable;
import java.util.Map;

/**
 * @author heipacker on 16-5-18.
 */
public interface BatClient extends Closeable {

    /**
     * Create a new trace scope.
     *
     * @param description The description of the new span to create.
     * @return The new trace scope.
     */
    TraceScope newScope(String description);

    /**
     * @param description
     * @param traceInfo
     * @return
     */
    TraceScope newScope(String description, TraceInfo traceInfo);

    /**
     * @param description  一个trace的描述信息
     * @param traceId
     * @param spanId
     * @param traceContext 存储一个trace链条的上下文信息
     * @return
     */
    TraceScope newScope(String description, String traceId, String spanId, Map<String, String> traceContext);

    /**
     *
     * @param span Span
     * @return the new trace scope
     */
    TraceScope newScope(Span span);

    /**
     * get current trace id
     *
     * @return
     */
    String getCurrentTraceId();

    /**
     * get next child span id of current trace id
     *
     * @return
     */
    String getNextChildSpanId();

    /**
     * get current trace context of current trace
     *
     * @return
     */
    ImmutableMap<String, String> getTraceContext();

    /**
     * Return a null trace scope.
     *
     * @return The null trace scope.
     */
    TraceScope newNullScope();

    /**
     * @return
     */
    Span getCurrentSpan();

    /**
     * @return
     */
    String getCurrentSpanId();

    void closeScope(TraceScope scope);

    @Override
    void close();
}
