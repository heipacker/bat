package com.dlmu.bat.client.concurrent;

import com.dlmu.bat.client.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
     * @param <V>         The subclass of callable.
     * @param callable    The callable to wrap.
     * @param description A description of the callable, or null if there
     *                    is no description.
     * @return The callable provided, wrapped if tracing, 'callable' if not.
     */
    public <V> Callable<V> wrap(Callable<V> callable, String description) {
        Span parentSpan = traceStatus.getCurrentSpan();
        if (parentSpan == null) {
            return callable;
        }
        return new TraceCallable<V>(description, parentSpan.getTraceId(), parentSpan.getSpanId(), parentSpan.getTraceContext(), callable);
    }

    /**
     * Wrap the runnable in a TraceRunnable, if tracing
     *
     * @param runnable    The runnable to wrap.
     * @param description A description of the runnable, or null if there is
     *                    no description.
     * @return The runnable provided, wrapped if tracing, 'runnable' if not.
     */
    public Runnable wrap(Runnable runnable, String description) {
        Span parentSpan = traceStatus.getCurrentSpan();
        if (parentSpan == null) {
            return runnable;
        }
        return new TraceRunnable(description, parentSpan.getTraceId(), parentSpan.getSpanId(), parentSpan.getTraceContext(), runnable);
    }

    /**
     * wrapper executorService
     *
     * @param impl
     * @return
     */
    public TraceExecutorService newTraceExecutorService(ExecutorService impl) {
        return newTraceExecutorService(impl, null);
    }

    /**
     * @param impl
     * @param scopeName
     * @return
     */
    public TraceExecutorService newTraceExecutorService(ExecutorService impl,
                                                        String scopeName) {
        return new TraceExecutorService(DTraceClientGetter.getClient(), scopeName, impl);
    }

}
