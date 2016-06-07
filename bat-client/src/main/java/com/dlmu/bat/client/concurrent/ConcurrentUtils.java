package com.dlmu.bat.client.concurrent;

import com.dlmu.bat.client.DefaultTraceStatus;
import com.dlmu.bat.client.Span;
import com.dlmu.bat.client.TraceStatus;
import com.google.common.collect.ImmutableMap;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class ConcurrentUtils {

    private static final TraceStatus traceStatus = DefaultTraceStatus.getInstance();

    /**
     * Wrap the callable in a TraceCallable, if tracing.
     *
     * @param <V>      The subclass of callable.
     * @param callable The callable to wrap.
     * @return The callable provided, wrapped if tracing, 'callable' if not.
     */
    public static <V> Callable<V> wrap(Callable<V> callable) {
        return wrap(callable, null);
    }

    /**
     * Wrap the callable in a TraceCallable, if tracing.
     *
     * @param <V>         The subclass of callable.
     * @param callable    The callable to wrap.
     * @param description A description of the callable, or null if there
     *                    is no description.
     * @return The callable provided, wrapped if tracing, 'callable' if not.
     */
    public static <V> Callable<V> wrap(Callable<V> callable, String description) {
        Span parentSpan = traceStatus.getCurrentSpan();
        if (parentSpan == null) {
            return callable;
        }
        String traceId = parentSpan.getTraceId();
        String spanId = parentSpan.getSpanId();
        ImmutableMap<String, String> traceContext = parentSpan.getTraceContext();
        return new TraceCallable<V>(description, traceId, spanId, traceContext, callable);
    }

    /**
     * Wrap the runnable in a TraceRunnable, if tracing
     *
     * @param runnable The runnable to wrap.
     * @return The runnable provided, wrapped if tracing, 'runnable' if not.
     */
    public static Runnable wrap(Runnable runnable) {
        return wrap(runnable, null);
    }

    /**
     * Wrap the runnable in a TraceRunnable, if tracing
     *
     * @param runnable    The runnable to wrap.
     * @param description A description of the runnable, or null if there is no description.
     * @return The runnable provided, wrapped if tracing, 'runnable' if not.
     */
    public static Runnable wrap(Runnable runnable, String description) {
        Span parentSpan = traceStatus.getCurrentSpan();
        if (parentSpan == null) {
            return runnable;
        }
        String traceId = parentSpan.getTraceId();
        String spanId = parentSpan.getSpanId();
        ImmutableMap<String, String> traceContext = parentSpan.getTraceContext();
        return new TraceRunnable(description, traceId, spanId, traceContext, runnable);
    }

    /**
     * wrapper executorService
     *
     * @param impl implements executorService
     * @return TraceExecutorService
     */
    public static TraceExecutorService newTraceExecutorService(ExecutorService impl) {
        return newTraceExecutorService(impl, null);
    }

    /**
     * @param impl implements executorService
     * @param description A description of the runnable, or null if there is no description.
     * @return TraceExecutorService
     */
    public static TraceExecutorService newTraceExecutorService(ExecutorService impl, String description) {
        return new TraceExecutorService(description, impl);
    }

}
