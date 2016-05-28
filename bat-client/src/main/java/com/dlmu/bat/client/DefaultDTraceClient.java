package com.dlmu.bat.client;

import com.dlmu.bat.client.receiver.LocalFileSpanReceiver;
import com.dlmu.bat.client.receiver.SpanReceiver;
import com.dlmu.bat.client.sampler.NeverSampler;
import com.dlmu.bat.client.sampler.Sampler;
import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.ClassUtils;
import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.conf.ConfigConstants;
import com.dlmu.bat.plugin.conf.Configuration;
import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * @author heipacker on 16-5-18.
 */
class DefaultDTraceClient implements DTraceClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDTraceClient.class);

    private static final TraceStatus traceStatus = DefaultTraceStatus.getInstance();

    private Configuration configuration;

    private SpanReceiver spanReceiver;

    /**
     * The currently active Samplers.
     * <p>
     * Arrays are immutable once set.  You must take the Tracer lock in order to
     * set this to a new array.  If this is null, the Tracer is closed.
     */
    private volatile Sampler curSampler;

    DefaultDTraceClient(Configuration configuration) {
        this.configuration = configuration;
        this.spanReceiver = loadReceiverType(this.configuration, null);
        this.configuration.addListener(new Configuration.ConfigurationListener() {
            @Override
            public void call(Configuration configuration) {
                String configSampler = configuration.get(ConfigConstants.SAMPLER_CLASS_KEY, NeverSampler.class.getName());
                if (curSampler != null && Objects.equal(configSampler, curSampler.getClass().getName())) {
                    return;
                }
                Class<?> configSamplerClass = ClassUtils.forName(configSampler);
                Preconditions.checkNotNull(configSamplerClass);
                try {
                    Constructor<?> configSamplerClassConstructor = configSamplerClass.getConstructor(Configuration.class);
                    curSampler = (Sampler) configSamplerClassConstructor.newInstance(DefaultDTraceClient.this.configuration);
                } catch (Exception e) {
                    logger.error("get constructor error", e);
                    throw Throwables.propagate(e);
                }
            }
        }, true);
    }

    /**
     * Given a SpanReceiver class name, return the existing instance of that span
     * receiver, if possible; otherwise, invoke the callable to create a new
     * instance.
     *
     * @param conf        The HTrace configuration.
     * @param classLoader The class loader to use.
     * @return The SpanReceiver.
     */
    private SpanReceiver loadReceiverType(Configuration conf, ClassLoader classLoader) {
        String className = conf.get(ConfigConstants.RECEIVER_TYPE_KEY, LocalFileSpanReceiver.class.getName());
        logger.trace(toString() + ": creating a new SpanReceiver of type " +
                className);
        SpanReceiver receiver = new SpanReceiver.Builder(conf).
                className(className).
                classLoader(classLoader == null ? SpanReceiver.Builder.class.getClassLoader() : classLoader).
                build();
        return receiver;
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
            if (parent != null) {//存在parent 说明不是root
                String traceId = parent.getTraceId();
                String spanId = parent.getSpanId();
                return newDefaultScope(description, traceId, spanId, parent.getTraceContext());
            } else {//root
                return newRootScope(description);
            }
        }
        if (Strings.isNullOrEmpty(traceInfo.traceId)) {
            return newRootScope(description);
        }
        return newDefaultScope(description, traceInfo.traceId, traceInfo.spanId, traceInfo.traceContext);
    }

    private TraceScope newDefaultScope(String description, String traceId, String spanId, Map<String, String> traceContext) {
        Span parent;
        if (getSample(traceId) == Sample.NO) {
            parent = new NullSpan(description, traceId, spanId);
        } else {
            parent = new MilliSpan(description, traceId, spanId, traceContext);
        }
        Span childSpan = parent.child(description);
        traceStatus.setCurrentSpan(childSpan);
        return new DefaultTraceScope(this, childSpan, parent);
    }

    private TraceScope newRootScope(String description) {
        Sample sample = getSample(null);
        String traceId = TracerId.next(configuration, sample);
        MilliSpan span = new MilliSpan(description, traceId, Constants.ROOT_SPANID);
        traceStatus.setCurrentSpan(span);
        return new DefaultTraceScope(this, span, null);
    }

    /**
     * 如果是空或者是NO_NEW_TRACEID则根据curSampler来判断是否需要采样
     * 如果traceId非空, 则通过traceId判断是否采样, 如果采样则也采样
     *
     * @param traceId
     * @return
     */
    private Sample getSample(String traceId) {
        if (Strings.isNullOrEmpty(traceId) || Objects.equal(Constants.NO_NEW_TRACEID, traceId)) {
            return curSampler.next() ? Sample.SHOULD : Sample.NO;
        }
        //override 当前应用能够覆盖traceId的采样
        if (configuration.getBoolean(ConfigConstants.OVERRIDE_SAMPLE_ENABLED_KEY, ConfigConstants.DEFAULT_OVERRIDE_SAMPLE_ENABLED)) {
            return curSampler.next() ? Sample.SHOULD : Sample.NO;
        }
        int lastIndexOf = traceId.lastIndexOf("_");
        if (lastIndexOf < 0) {
            return Sample.NO;
        }
        char suffix = traceId.charAt(lastIndexOf + 1);
        Optional<Sample> sampleOptional = Sample.getSample(suffix);
        if (sampleOptional.isPresent()) {
            return sampleOptional.get();
        }
        return Sample.NO;
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
        return new NullScope(this);
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
        if (spanReceiver == null) {
            throwClientError(toString() + " is closed.");
        }
        traceStatus.setCurrentSpan(scope.getParentSpan());
        scope.setParentSpan(null);
        Span span = scope.getSpan();
        span.stop();
        spanReceiver.receiveSpan((BaseSpan) span);
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
        if (spanReceiver == null) {
            return;
        }
        try {
            spanReceiver.close();
        } catch (IOException e) {
            logger.error("close spanReceiver error", e);
        }
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
