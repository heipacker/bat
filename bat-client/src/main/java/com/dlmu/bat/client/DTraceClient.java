package com.dlmu.bat.client;

import com.google.common.collect.ImmutableMap;

import java.io.Closeable;
import java.util.Map;

/**
 * @author heipacker on 16-5-18.
 */
public interface DTraceClient extends Closeable {

    TraceScope newScope(String description, String traceId, String spanId, Map<String, String> traceContext);

    /**
     * @param description
     * @param traceInfo
     * @return
     */
    TraceScope newScope(String description, TraceInfo traceInfo);

    /**
     * Create a new trace scope.
     * <p>
     * If there are no scopes above the current scope, we will apply our
     * configured samplers. Otherwise, we will create a trace Span only if this thread
     * is already tracing.
     *
     * @param description The description of the new span to create.
     * @return The new trace scope.
     */
    TraceScope newScope(String description);

    String getCurrentTraceId();

    String getNextChildSpanId();

    ImmutableMap<String, String> getTraceContext();

    /**
     * Return a null trace scope.
     *
     * @return The null trace scope.
     */
    TraceScope newNullScope();

    Span getCurrentSpan();

    String getCurrentSpanId();

    void closeScope(TraceScope scope);

    @Override
    void close();
}
