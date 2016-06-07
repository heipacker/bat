package com.dlmu.bat.client.concurrent;

import com.dlmu.bat.client.BatClientGetter;
import com.dlmu.bat.client.TraceScope;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Wrap a Callable with a Span that survives a change in threads.
 */
public class TraceCallable<V> implements Callable<V> {

    private String description;
    private String traceId;
    private String spanId;
    private Map<String, String> traceContext;
    private final Callable<V> callable;

    public TraceCallable(String traceId, String spanId, Map<String, String> traceContext, Callable<V> callable) {
        this(null, traceId, spanId, traceContext, callable);
    }

    public TraceCallable(String description, String traceId, String spanId, Map<String, String> traceContext, Callable<V> callable) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceContext = traceContext;
        this.callable = callable;
        this.description = description;
    }

    @Override
    public V call() throws Exception {
        if (traceId == null) {
            return callable.call();
        } else {
            String description = this.description == null ? Thread.currentThread().getName() : this.description;
            TraceScope traceScope = BatClientGetter.getClient().newScope(description, traceId, spanId, traceContext);
            try {
                return callable.call();
            } finally {
                traceScope.close();
            }
        }
    }

    public Callable<V> getImpl() {
        return callable;
    }
}
