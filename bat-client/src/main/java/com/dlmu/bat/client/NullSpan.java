package com.dlmu.bat.client;

import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.TimelineAnnotation;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author heipacker
 * @date 16-5-24.
 */
public class NullSpan extends BaseSpan implements Span {

    private final AtomicInteger index = new AtomicInteger(0);

    private Map<String, String> traceContext;

    public NullSpan(String description, String traceId, String spanId) {
        super(description, traceId, spanId);
    }

    public NullSpan(String description, String traceId, String spanId, Map<String, String> traceContext) {
        this(description, traceId, spanId);
        this.traceContext = Maps.newHashMap(traceContext);
    }

    /**
     * The block has completed, stop the clock
     */
    @Override
    public void stop() {
        stop = currentTimeMillis();
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Get the span start time.
     *
     * @return The start time, in approximate milliseconds since the epoch.
     */
    @Override
    public long getStartTimeMillis() {
        return start;
    }

    /**
     * Get the span stop time.
     *
     * @return The stop time, in approximate milliseconds since the epoch.
     */
    @Override
    public long getStopTimeMillis() {
        return stop;
    }

    /**
     * Return the total amount of time elapsed since start was called, if running,
     * or difference between stop and start
     *
     * @return The elapsed time in milliseconds.
     */
    @Override
    public long getAccumulatedMillis() {
        if (start == 0)
            return 0;
        if (stop > 0)
            return stop - start;
        return currentTimeMillis() - start;

    }

    /**
     * Has the span been started and not yet stopped?
     *
     * @return True if the span is still running (has no stop time).
     */
    @Override
    public boolean isRunning() {
        return start != 0 && stop == 0;
    }

    /**
     * Return a textual description of this span.
     *
     * @return The description of this span.  Will never be null.
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * A pseudo-unique (random) number assigned to this span instance.
     *
     * @return The spanID.  This object is immutable and is safe to access
     * from multiple threads.
     */
    @Override
    public String getSpanId() {
        return spanId;
    }

    private String generateNextChildSpanId() {
        return this.spanId + "." + index.incrementAndGet();
    }

    /**
     * Create a child span of this span with the given description
     *
     * @param description
     * @return A new child span.
     */
    @Override
    public Span child(String description) {
        return new MilliSpan(description, traceId, generateNextChildSpanId());
    }

    @Override
    public String getChildNextId() {
        return generateNextChildSpanId();
    }

    /**
     * Add a data annotation associated with this span
     *
     * @param key   The key to set.
     * @param value The value to set.
     */
    @Override
    public void addKVAnnotation(String key, String value) {

    }

    /**
     * Add a timeline annotation associated with this span
     *
     * @param msg The annotation to set.  It will be associated with
     *            the current time.
     */
    @Override
    public void addTimelineAnnotation(String msg) {

    }

    /**
     * Get the key-value annotations associated with this span.
     *
     * @return The annotation map in read-only form.
     * Will never be null.
     */
    @Override
    public Map<String, String> getKVAnnotations() {
        return null;
    }

    /**
     * Get the timeline annotation list.
     *
     * @return The annotation list in read-only form.
     * Will never be null.
     */
    @Override
    public List<TimelineAnnotation> getTimelineAnnotations() {
        return null;
    }

    @Override
    public void addTraceContext(String key, String value) {
        if (this.traceContext == null) {
            this.traceContext = Maps.<String, String>newHashMap();
        }
        traceContext.put(key, value);
    }

    @Override
    public ImmutableMap<String, String> getTraceContext() {
        if (this.traceContext == null) {
            return ImmutableMap.of();
        }
        return ImmutableMap.copyOf(traceContext);
    }

    /**
     * Return a unique id for the process from which this Span originated.
     *
     * @return The batClient id.  Will never be null.
     */
    @Override
    public String getTraceId() {
        return traceId;
    }

    /**
     * Set the batClient id of a span.
     *
     * @param traceId The batClient ID to set.
     */
    @Override
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
