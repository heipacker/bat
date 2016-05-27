package com.dlmu.bat.client;

import com.dlmu.bat.client.receiver.SpanReceiver;
import com.dlmu.bat.client.sampler.Sampler;
import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.conf.DTraceConfiguration;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

/**
 * @author heipacker on 16-5-18.
 */
public class DefaultDTraceClient implements DTraceClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDTraceClient.class);

    private static final TraceStatus traceStatus = DefaultTraceStatus.getInstance();

    private DTraceConfiguration configuration;
    /**
     * The TracerPool which this Tracer belongs to.
     * <p>
     * This gets set to null after the Tracer is closed in order to catch some
     * use-after-close errors.  Note that we do not synchronize access on this
     * field, since it only changes when the Tracer is closed, and the Tracer
     * should not be used after that.
     */
    private TracerPool tracerPool = TracerPool.getGlobalTracerPool();

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

    public DefaultDTraceClient(DTraceConfiguration configuration) {
        this.configuration = configuration;
        this.nullScope = new NullScope(this);
    }

    /**
     * Log a client error, and throw an exception.
     *
     * @param str The message to use in the log and the exception.
     */
    static void throwClientError(String str) {
        logger.error(str);
        throw new RuntimeException(str);
    }

    public TraceScope newScope(String description, String traceId, String spanId, Map<String, String> traceContext) {
        return newScope(description, new TraceInfo(traceId, spanId, traceContext));
    }

    /**
     * @param description
     * @param traceInfo
     * @return
     */
    public TraceScope newScope(String description, TraceInfo traceInfo) {
        if (traceInfo == null || Objects.equal(traceInfo.traceId, Constants.NO_NEW_TRACEID)) {//root
            Span parent = traceStatus.getCurrentSpan();
            if (parent != null) {
                String traceId = parent.getTraceId();
                String spanId = parent.getSpanId();
                ImmutableMap<String, String> traceContext = parent.getTraceContext();
                return newScope(description, new TraceInfo(traceId, spanId, traceContext));
            }
            Sample sample = getSample(description, null);
            String traceId = TracerId.next(configuration, sample);
            MilliSpan span = new MilliSpan(description, traceId, Constants.ROOT_SPANID);
            traceStatus.setCurrentSpan(span);
            return new DefaultTraceScope(this, span, null);
        }
        MilliSpan parent = new MilliSpan(description, traceInfo.traceId, traceInfo.spanId);
        Span childSpan = parent.child(description);
        traceStatus.setCurrentSpan(childSpan);
        return new DefaultTraceScope(this, childSpan, parent);
    }

    private Sample getSample(String description, String traceId) {
        return Sample.MUST;
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
        return newScope(description, null);
    }

    public String getCurrentTraceId() {
        return traceStatus.getCurrentSpan() == null ? null : traceStatus.getCurrentSpan().getTraceId();
    }

    public String getNextChildSpanId() {
        return traceStatus.getCurrentSpan() == null ? null : traceStatus.getCurrentSpan().getChildNextId();
    }

    public ImmutableMap<String, String> getTraceContext() {
        return traceStatus.getCurrentSpan() == null ? null : traceStatus.getCurrentSpan().getTraceContext();
    }

    /**
     * Return a null trace scope.
     *
     * @return The null trace scope.
     */
    public TraceScope newNullScope() {
        return nullScope;
    }


    public TracerPool getTracerPool() {
        if (tracerPool == null) {
            throwClientError(toString() + " is closed.");
        }
        return tracerPool;
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

    static void detachScope(TraceScope scope) {
        Span curSpan = traceStatus.getCurrentSpan();
        if (curSpan != scope) {
            throwClientError("Can't detach TraceScope for " +
                    scope.getSpan() + " because it is not the current " +
                    "TraceScope in thread " + Thread.currentThread().getName());
        }
        traceStatus.setCurrentSpan(scope.getParentSpan());
    }

    static void reattachScope(TraceScope scope) {
        Span parent = traceStatus.getCurrentSpan();
        traceStatus.setCurrentSpan(scope.getSpan());
        scope.setParentSpan(parent);
    }

    @Override
    public void closeScope(TraceScope scope) {
        Span curSpan = traceStatus.getCurrentSpan();
        if (curSpan != scope.getSpan()) {
            throwClientError("Can't close TraceScope for " +
                    scope.getSpan() + " because it is not the current " +
                    "TraceScope in thread " + Thread.currentThread().getName());
        }
        if (tracerPool == null) {
            throwClientError(toString() + " is closed.");
        }
        SpanReceiver[] receivers = tracerPool.getReceivers();
        if (receivers == null) {
            throwClientError(toString() + " is closed.");
        }
        traceStatus.setCurrentSpan(scope.getParentSpan());
        scope.setParentSpan(null);
        Span span = scope.getSpan();
        span.stop();
        for (SpanReceiver receiver : receivers) {
            receiver.receiveSpan((BaseSpan) span);
        }
    }

    public Span getCurrentSpan() {
        return traceStatus.getCurrentSpan();
    }

    public String getCurrentSpanId() {
        Span curSpan = traceStatus.getCurrentSpan();
        if (curSpan == null) {
            return "";
        } else {
            return curSpan.getSpanId();
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
}
