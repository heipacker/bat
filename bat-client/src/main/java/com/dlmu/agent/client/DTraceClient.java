package com.dlmu.agent.client;

import com.dlmu.agent.client.concurrent.TraceCallable;
import com.dlmu.agent.client.concurrent.TraceExecutorService;
import com.dlmu.agent.client.concurrent.TraceRunnable;
import com.dlmu.agent.client.receiver.SpanReceiver;
import com.dlmu.agent.client.sampler.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * @author heipacker on 16-5-18.
 */
public class DTraceClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DTraceClient.class);

    public static DTraceClient getClient() {
        return new DTraceClient("", null, null);
    }

    private DTraceClient(String tracerId, TracerPool tracerPool, Sampler[] curSamplers) {
        this.tracerId = tracerId;
        this.tracerPool = tracerPool;
        this.threadContext = new DTraceClient.ThreadLocalContext();
        this.nullScope = new NullScope(this);
        this.curSamplers = curSamplers;
    }


    /**
     * The thread-specific context for this Tracer.
     * <p>
     * This tracks the current number of trace scopes in a particular thread
     * created by this dTraceClient.  We use this to apply our samplers only for the
     * "top-level" spans.
     * <p>
     * Note that we can't put the TraceScope objects themselves in this context,
     * since we need to be able to use TraceScopes created by other Tracers, and
     * this context is per-Tracer.
     */
    private static class ThreadContext {
        private long depth;

        ThreadContext() {
            this.depth = 0;
        }

        boolean isTopLevel() {
            return (depth == 0);
        }

        void pushScope() {
            depth++;
        }

        TraceScope pushNewScope(DTraceClient dTraceClient, Span span, TraceScope parentScope) {
            TraceScope scope = new TraceScope(dTraceClient, span, parentScope);
            threadLocalScope.set(scope);
            depth++;
            return scope;
        }

        void popScope() {
            if (depth <= 0) {
                throwClientError("There were more trace scopes closed than " +
                        "were opened.");
            }
            depth--;
        }
    }

    /**
     * A subclass of ThreadLocal that starts off with a non-null initial value in
     * each thread.
     */
    private static class ThreadLocalContext extends ThreadLocal<DTraceClient.ThreadContext> {
        @Override
        protected DTraceClient.ThreadContext initialValue() {
            return new DTraceClient.ThreadContext();
        }
    }

    /**
     * The current trace scope.  This is global, so it is shared amongst all
     * libraries using HTrace.
     */
    final static ThreadLocal<TraceScope> threadLocalScope = new ThreadLocal<TraceScope>();
    /**
     * An empty array of SpanId objects.  Can be used rather than constructing a
     * new object whenever we need an empty array.
     */
    private static final SpanId EMPTY_PARENT_ARRAY[] = new SpanId[0];

    /**
     * The tracerId.
     */
    private final String tracerId;

    /**
     * The TracerPool which this Tracer belongs to.
     * <p>
     * This gets set to null after the Tracer is closed in order to catch some
     * use-after-close errors.  Note that we do not synchronize access on this
     * field, since it only changes when the Tracer is closed, and the Tracer
     * should not be used after that.
     */
    private TracerPool tracerPool;

    /**
     * The current thread-local context for this particualr Tracer.
     */
    private final DTraceClient.ThreadLocalContext threadContext;

    /**
     * The NullScope instance for this Tracer.
     */
    private final NullScope nullScope;

    /**
     * The currently active Samplers.
     * <p>
     * Arrays are immutable once set.  You must take the Tracer lock in order to
     * set this to a new array.  If this is null, the Tracer is closed.
     */
    private volatile Sampler[] curSamplers;

    /**
     * Log a client error, and throw an exception.
     *
     * @param str The message to use in the log and the exception.
     */
    static void throwClientError(String str) {
        logger.error(str);
        throw new RuntimeException(str);
    }

    /**
     * @return If the current thread is tracing, this function returns the Tracer that is
     * being used; otherwise, it returns null.
     */
    public static DTraceClient curThreadTracer() {
        TraceScope traceScope = threadLocalScope.get();
        if (traceScope == null) {
            return null;
        }
        return traceScope.dTraceClient;
    }

    private TraceScope newScopeImpl(DTraceClient.ThreadContext context, String description) {
        Span span = new MilliSpan.Builder().
                tracerId(tracerId).
                begin(System.currentTimeMillis()).
                description(description).
                parents(EMPTY_PARENT_ARRAY).
                spanId(SpanId.fromRandom()).
                build();
        return context.pushNewScope(this, span, null);
    }

    private TraceScope newScopeImpl(DTraceClient.ThreadContext context, String description,
                                    TraceScope parentScope) {
        SpanId parentId = parentScope.getSpan().getSpanId();
        Span span = new MilliSpan.Builder().
                tracerId(tracerId).
                begin(System.currentTimeMillis()).
                description(description).
                parents(new SpanId[]{parentId}).
                spanId(parentId.newChildId()).
                build();
        return context.pushNewScope(this, span, parentScope);
    }

    private TraceScope newScopeImpl(DTraceClient.ThreadContext context, String description,
                                    SpanId parentId) {
        Span span = new MilliSpan.Builder().
                tracerId(tracerId).
                begin(System.currentTimeMillis()).
                description(description).
                parents(new SpanId[]{parentId}).
                spanId(parentId.newChildId()).
                build();
        return context.pushNewScope(this, span, null);
    }

    private TraceScope newScopeImpl(DTraceClient.ThreadContext context, String description,
                                    TraceScope parentScope, SpanId secondParentId) {
        SpanId parentId = parentScope.getSpan().getSpanId();
        Span span = new MilliSpan.Builder().
                tracerId(tracerId).
                begin(System.currentTimeMillis()).
                description(description).
                parents(new SpanId[]{parentId, secondParentId}).
                spanId(parentId.newChildId()).
                build();
        return context.pushNewScope(this, span, parentScope);
    }

    /**
     * Create a new trace scope.
     * <p>
     * If there are no scopes above the current scope, we will apply our
     * configured samplers. Otherwise, we will create a trace Span only if this thread
     * is already tracing, or if the passed parentID was valid.
     *
     * @param description The description of the new span to create.
     * @param parentId    If this is a valid span ID, it will be added to
     *                    the parents of the new span we create.
     * @return The new trace scope.
     */
    public TraceScope newScope(String description, SpanId parentId) {
        TraceScope parentScope = threadLocalScope.get();
        DTraceClient.ThreadContext context = threadContext.get();
        if (parentScope != null) {
            if (parentId.isValid() &&
                    (!parentId.equals(parentScope.getSpan().getSpanId()))) {
                return newScopeImpl(context, description, parentScope, parentId);
            } else {
                return newScopeImpl(context, description, parentScope);
            }
        } else if (parentId.isValid()) {
            return newScopeImpl(context, description, parentId);
        }
        if (!context.isTopLevel()) {
            context.pushScope();
            return nullScope;
        }
        if (!sample()) {
            context.pushScope();
            return nullScope;
        }
        return newScopeImpl(context, description);
    }

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
    public TraceScope newScope(String description) {
        TraceScope parentScope = threadLocalScope.get();
        DTraceClient.ThreadContext context = threadContext.get();
        if (parentScope != null) {
            return newScopeImpl(context, description, parentScope);
        }
        if (!context.isTopLevel()) {
            context.pushScope();
            return nullScope;
        }
        if (!sample()) {
            context.pushScope();
            return nullScope;
        }
        return newScopeImpl(context, description);
    }

    /**
     * Return a null trace scope.
     *
     * @return The null trace scope.
     */
    public TraceScope newNullScope() {
        DTraceClient.ThreadContext context = threadContext.get();
        context.pushScope();
        return nullScope;
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
    public <V> Callable<V> wrap(Callable<V> callable, String description) {
        TraceScope parentScope = threadLocalScope.get();
        if (parentScope == null) {
            return callable;
        }
        return new TraceCallable<V>(this, parentScope.getSpanId(), callable, description);
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
        TraceScope parentScope = threadLocalScope.get();
        if (parentScope == null) {
            return runnable;
        }
        return new TraceRunnable(this, parentScope, runnable, description);
    }

    public TraceExecutorService newTraceExecutorService(ExecutorService impl) {
        return newTraceExecutorService(impl, null);
    }

    public TraceExecutorService newTraceExecutorService(ExecutorService impl,
                                                        String scopeName) {
        return new TraceExecutorService(this, scopeName, impl);
    }

    public TracerPool getTracerPool() {
        if (tracerPool == null) {
            throwClientError(toString() + " is closed.");
        }
        return tracerPool;
    }

    public String getTracerId() {
        return tracerId;
    }
    /**
     * Returns an object that will trace all calls to itself.
     */
    @SuppressWarnings("unchecked")
    <T, V> T createProxy(final T instance) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object obj, Method method, Object[] args)
                    throws Throwable {
                try (TraceScope scope = DTraceClient.this.newScope(method.getName());) {
                    return method.invoke(instance, args);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    throw ex;
                }
            }
        };
        return (T) Proxy.newProxyInstance(instance.getClass().getClassLoader(),
                instance.getClass().getInterfaces(), handler);
    }

    /**
     * Return true if we should create a new top-level span.
     * <p>
     * We will create the span if any configured sampler returns true.
     */
    private boolean sample() {
        Sampler[] samplers = curSamplers;
        for (Sampler sampler : samplers) {
            if (sampler.next()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an array of all the current Samplers.
     * <p>
     * Note that if the current Samplers change, those changes will not be
     * reflected in this array.  In other words, this array may be stale.
     *
     * @return The current samplers.
     */
    public Sampler[] getSamplers() {
        return curSamplers;
    }

    /**
     * Add a new Sampler.
     *
     * @param sampler The new sampler to add.
     *                You cannot add a particular Sampler object more
     *                than once.  You may add multiple Sampler objects
     *                of the same type, although this is not recommended.
     * @return True if the sampler was added; false if it already had
     * been added earlier.
     */
    public synchronized boolean addSampler(Sampler sampler) {
        if (tracerPool == null) {
            throwClientError(toString() + " is closed.");
        }
        Sampler[] samplers = curSamplers;
        for (int i = 0; i < samplers.length; i++) {
            if (samplers[i] == sampler) {
                return false;
            }
        }
        Sampler[] newSamplers =
                Arrays.copyOf(samplers, samplers.length + 1);
        newSamplers[samplers.length] = sampler;
        curSamplers = newSamplers;
        return true;
    }

    /**
     * Remove a Sampler.
     *
     * @param sampler The sampler to remove.
     * @return True only if the sampler was removed.
     */
    public synchronized boolean removeSampler(Sampler sampler) {
        if (tracerPool == null) {
            throwClientError(toString() + " is closed.");
        }
        Sampler[] samplers = curSamplers;
        for (int i = 0; i < samplers.length; i++) {
            if (samplers[i] == sampler) {
                Sampler[] newSamplers = new Sampler[samplers.length - 1];
                System.arraycopy(samplers, 0, newSamplers, 0, i);
                System.arraycopy(samplers, i + 1, newSamplers, i,
                        samplers.length - i - 1);
                curSamplers = newSamplers;
                return true;
            }
        }
        return false;
    }

    void detachScope(TraceScope scope) {
        TraceScope curScope = threadLocalScope.get();
        if (curScope != scope) {
            throwClientError("Can't detach TraceScope for " +
                    scope.getSpan().toJson() + " because it is not the current " +
                    "TraceScope in thread " + Thread.currentThread().getName());
        }
        DTraceClient.ThreadContext context = threadContext.get();
        context.popScope();
        threadLocalScope.set(scope.getParent());
    }

    void reattachScope(TraceScope scope) {
        TraceScope parent = threadLocalScope.get();
        DTraceClient.threadLocalScope.set(scope);
        DTraceClient.ThreadContext context = threadContext.get();
        context.pushScope();
        scope.setParent(parent);
    }

    void closeScope(TraceScope scope) {
        TraceScope curScope = threadLocalScope.get();
        if (curScope != scope) {
            throwClientError("Can't close TraceScope for " +
                    scope.getSpan().toJson() + " because it is not the current " +
                    "TraceScope in thread " + Thread.currentThread().getName());
        }
        if (tracerPool == null) {
            throwClientError(toString() + " is closed.");
        }
        SpanReceiver[] receivers = tracerPool.getReceivers();
        if (receivers == null) {
            throwClientError(toString() + " is closed.");
        }
        DTraceClient.ThreadContext context = threadContext.get();
        context.popScope();
        threadLocalScope.set(scope.getParent());
        scope.setParent(null);
        Span span = scope.getSpan();
        span.stop();
        for (SpanReceiver receiver : receivers) {
            receiver.receiveSpan(span);
        }
    }

    void popNullScope() {
        TraceScope curScope = threadLocalScope.get();
        if (curScope != null) {
            throwClientError("Attempted to close an empty scope, but it was not " +
                    "the current thread scope in thread " +
                    Thread.currentThread().getName());
        }
        DTraceClient.ThreadContext context = threadContext.get();
        context.popScope();
    }

    public static Span getCurrentSpan() {
        TraceScope curScope = threadLocalScope.get();
        if (curScope == null) {
            return null;
        } else {
            return curScope.getSpan();
        }
    }

    public static SpanId getCurrentSpanId() {
        TraceScope curScope = threadLocalScope.get();
        if (curScope == null) {
            return SpanId.INVALID;
        } else {
            return curScope.getSpan().getSpanId();
        }
    }

    @Override
    public synchronized void close() {
        if (tracerPool == null) {
            return;
        }
        curSamplers = new Sampler[0];
        tracerPool.removeTracer(this);
    }

    /**
     * Get the hash code of a Tracer object.
     * <p>
     * This hash code is based on object identity.
     * This is used in TracerPool to create a hash table of Tracers.
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Compare two dTraceClient objects.
     * <p>
     * Tracer objects are always compared by object equality.
     * This is used in TracerPool to create a hash table of Tracers.
     */
    @Override
    public boolean equals(Object other) {
        return (this == other);
    }

    @Override
    public String toString() {
        return "Tracer(" + tracerId + ")";
    }

    public static void main(String[] args) {
        System.out.println(DTraceClient.class.getPackage().getName());
    }
}
