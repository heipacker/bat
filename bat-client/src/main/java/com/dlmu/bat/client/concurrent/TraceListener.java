package com.dlmu.bat.client.concurrent;

import com.dlmu.bat.client.BatClientGetter;
import com.dlmu.bat.client.Span;
import com.dlmu.bat.client.TraceScope;
import com.dlmu.bat.common.Constants;

public class TraceListener implements Runnable {

    private Runnable target;
    private Span span;

    public TraceListener(Runnable target, Span span) {
        this.target = target;
        this.span = span;
    }

    @Override
    public void run() {
        TraceScope parentTraceScope = BatClientGetter.getClient().newScope(span);
        try {
            TraceScope traceScope = BatClientGetter.getClient().newScope(Thread.currentThread().getName());
            try {
                target.run();
                traceScope.addKVAnnotation(Constants.TRACE_STATUS, Constants.TRACE_STATUS_OK);
            } catch (Exception e) {
                traceScope.addKVAnnotation(Constants.EXCEPTION_KEY, e.getMessage());
                traceScope.addKVAnnotation(Constants.TRACE_STATUS, Constants.TRACE_STATUS_ERROR);
                throw new RuntimeException(e);
            } finally {
                traceScope.close();
            }
        } finally {
            parentTraceScope.close();
        }
    }
}
