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
class DefaultBatClient implements BatClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBatClient.class);

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

    DefaultBatClient(Configuration configuration) {
        this.configuration = configuration;
        this.spanReceiver = loadReceiverType(this.configuration, null);
        this.configuration.addListener(new Configuration.ConfigurationListener() {
            @Override
            public void call(Configuration configuration) {
                String configSamplerClassName = configuration.get(ConfigConstants.SAMPLER_CLASS_KEY, NeverSampler.class.getName());
                if (curSampler != null && Objects.equal(configSamplerClassName, curSampler.getClass().getName())) {
                    return;
                }
                Class<?> configSamplerClass = ClassUtils.forName(configSamplerClassName);
                Preconditions.checkNotNull(configSamplerClass);
                try {
                    Constructor<?> configSamplerClassConstructor = configSamplerClass.getConstructor(Configuration.class);
                    curSampler = (Sampler) configSamplerClassConstructor.newInstance(DefaultBatClient.this.configuration);
                } catch (Exception e) {
                    logger.error("get constructor error", e);
                    throw Throwables.propagate(e);
                }
            }
        }, true);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }));
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
        logger.debug(toString() + ": creating a new SpanReceiver of type " + className);
        return new SpanReceiver.Builder(conf).
                className(className).
                classLoader(classLoader == null ? SpanReceiver.Builder.class.getClassLoader() : classLoader).
                build();
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

    /**
     * Create a new trace scope.
     *
     * @param description The description of the new span to create.
     * @return The new trace scope.
     */
    public TraceScope newScope(String description) {
        return newScope(description, null);
    }

    /**
     * if traceInfo is null or trace id equals to @see com.dlmu.bat.common.Constants.NO_NEW_TRACEID, it
     * mean that this may be a root scope or one rpc/http invoke etc.
     * else this is a sub trace scope
     *
     * @param description The description of the new span to create.
     * @param traceInfo   trace information
     * @return The new trace scope.
     */
    public TraceScope newScope(String description, TraceInfo traceInfo) {
        if (traceInfo == null || Objects.equal(traceInfo.traceId, Constants.NO_NEW_TRACEID)
                || Strings.isNullOrEmpty(traceInfo.traceId)) {//root
            Span parentSpan = traceStatus.getCurrentSpan();
            if (parentSpan != null) {//存在parent 说明不是root 这种情况可以是用注解搞的
                String traceId = parentSpan.getTraceId();
                String spanId = parentSpan.getSpanId();
                return newDefaultScope(description, traceId, spanId, parentSpan.getTraceContext());
            } else {//root
                return newRootScope(description);
            }
        }
        return newDefaultScope(description, traceInfo.traceId, traceInfo.spanId, traceInfo.traceContext);
    }

    /**
     * create a new trace id every times
     *
     * @param description The description of the new span to create.
     * @return The new trace scope.
     */
    private TraceScope newRootScope(String description) {
        MilliSpan span = new MilliSpan(description, TraceId.next(getSample(null)), Constants.ROOT_SPANID);
        traceStatus.setCurrentSpan(span);
        return new DefaultTraceScope(this, span, null);
    }

    /**
     * todo 需要考虑父invoke(A)开启采样, 但是当前的(B)不开启采样, 下游(C)开启采样, 这个如何保证不断了
     *
     * @param description  The description of the new span to create.
     * @param traceId      one trace id
     * @param spanId       one trace id
     * @param traceContext 存储一个trace链条的上下文信息
     * @return The new trace scope.
     */
    private TraceScope newDefaultScope(String description, String traceId, String spanId, Map<String, String> traceContext) {
        Span parentSpan;
        if (getSample(traceId) == Sample.NO) {
            parentSpan = new NullSpan(description, traceId, spanId, traceContext);
        } else {
            parentSpan = new MilliSpan(description, traceId, spanId, traceContext);
        }
        Span childSpan = parentSpan.child(description);
        traceStatus.setCurrentSpan(childSpan);
        return new DefaultTraceScope(this, childSpan, parentSpan);
    }

    /**
     * @param description  一个trace的描述信息
     * @param traceId      one trace id
     * @param spanId       one span id of the trace
     * @param traceContext 存储一个trace链条的上下文信息
     * @return The new trace scope.
     */
    public TraceScope newScope(String description, String traceId, String spanId, Map<String, String> traceContext) {
        return newScope(description, new TraceInfo(traceId, spanId, traceContext));
    }

    /**
     * Return a null trace scope.
     *
     * @return The null trace scope.
     */
    public TraceScope newNullScope() {
        return new NullScope(this);
    }

    /**
     * 如果是空或者是NO_NEW_TRACEID则根据curSampler来判断是否需要采样
     * 如果traceId非空, 则通过traceId判断是否采样, 如果采样则也采样
     *
     * @param traceId trace id
     * @return Sample object
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

    static void detachScope(TraceScope scope) {
        Span curSpan = traceStatus.getCurrentSpan();
        if (curSpan != scope) {
            throwClientError("Can't detach TraceScope for " + scope.getSpan() + " because it is not the current " +
                    "TraceScope in thread " + Thread.currentThread().getName());
        }
        traceStatus.setCurrentSpan(scope.getParentSpan());
    }

    static void reattachScope(TraceScope scope) {
        Span parent = traceStatus.getCurrentSpan();
        traceStatus.setCurrentSpan(scope.getSpan());
        scope.setParentSpan(parent);
    }

    public String getCurrentTraceId() {
        return traceStatus.getCurrentSpan() == null ? null : traceStatus.getCurrentSpan().getTraceId();
    }

    public ImmutableMap<String, String> getTraceContext() {
        return traceStatus.getCurrentSpan() == null ? null : traceStatus.getCurrentSpan().getTraceContext();
    }

    public Span getCurrentSpan() {
        return traceStatus.getCurrentSpan();
    }

    public String getCurrentSpanId() {
        return traceStatus.getCurrentSpan() == null ? null : traceStatus.getCurrentSpan().getSpanId();
    }

    public String getNextChildSpanId() {
        return traceStatus.getCurrentSpan() == null ? null : traceStatus.getCurrentSpan().getChildNextId();
    }

    @Override
    public void closeScope(TraceScope scope) {
        Span curSpan = traceStatus.getCurrentSpan();
        if (curSpan != scope.getSpan()) {
            throwClientError("Can't close TraceScope for " + scope.getSpan() + " because it is not the current " +
                    "TraceScope in thread " + Thread.currentThread().getName());
        }
        if (spanReceiver == null) {
            throwClientError(toString() + " is closed.");
        }
        traceStatus.setCurrentSpan(scope.getParentSpan());
        Span span = scope.getSpan();
        span.stop();
        spanReceiver.receiveSpan((BaseSpan) span);
    }

    @Override
    public synchronized void close() {
        try {
            if (spanReceiver != null) {
                spanReceiver.close();
            }
        } catch (IOException e) {
            logger.error("close spanReceiver error", e);
        }
    }

    /**
     * Get the hash code of a Tracer object.
     * <p>
     * This hash code is based on object identity.
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Compare two batClient objects.
     * <p>
     * Tracer objects are always compared by object equality.
     */
    @Override
    public boolean equals(Object other) {
        return (this == other);
    }
}
