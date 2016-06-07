package com.dlmu.bat.client;

/**
 * store current trace status
 *
 * @author heipacker
 * @date 16-5-27.
 */
public class DefaultTraceStatus implements TraceStatus {

    private static final TraceStatus instance = new DefaultTraceStatus();

    public static TraceStatus getInstance() {
        return instance;
    }

    private final ThreadLocal<Span> threadLocal = new ThreadLocal<Span>();

    private DefaultTraceStatus() {

    }

    @Override
    public Span getCurrentSpan() {
        return threadLocal.get();
    }

    @Override
    public void setCurrentSpan(Span span) {
        threadLocal.set(span);
    }

    @Override
    public void remove() {
        threadLocal.remove();
    }
}
