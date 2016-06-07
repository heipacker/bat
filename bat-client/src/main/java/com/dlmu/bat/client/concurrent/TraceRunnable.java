package com.dlmu.bat.client.concurrent;

import com.dlmu.bat.client.BatClientGetter;
import com.dlmu.bat.client.TraceScope;

import java.util.Map;

/**
 * Wrap a Runnable with a Span that survives a change in threads.
 */
public class TraceRunnable implements Runnable {

    private String description;
    private String traceId;
    private String spanId;
    private Map<String, String> traceContext;
    private Runnable runnable;

    public TraceRunnable(String traceId, String spanId, Map<String, String> traceContext, Runnable runnable) {
        this(null, traceId, spanId, traceContext, runnable);
    }

    public TraceRunnable(String description, String traceId, String spanId, Map<String, String> traceContext, Runnable runnable) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceContext = traceContext;
        this.runnable = runnable;
        this.description = description;
    }

    @Override
    public void run() {
        if (traceId == null) {
            runnable.run();
        } else {
            String description = this.description == null ? Thread.currentThread().getName() : this.description;
            TraceScope traceScope = BatClientGetter.getClient().newScope(description, traceId, spanId, traceContext);
            try {
                runnable.run();
            } finally {
                traceScope.close();
            }
        }
    }

    public Runnable getRunnable() {
        return runnable;
    }
}
