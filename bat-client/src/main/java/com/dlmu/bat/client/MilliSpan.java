/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dlmu.bat.client;

import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.NetUtil;
import com.dlmu.bat.common.TimelineAnnotation;
import com.dlmu.bat.common.tname.Utils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Span implementation that stores its information in milliseconds since the
 * epoch.
 */
public class MilliSpan extends BaseSpan implements Span {

    private final AtomicInteger index = new AtomicInteger(0);

    private Map<String, String> traceContext = null;

    public MilliSpan(String description, String traceId, String spanId) {
        super(description, traceId, spanId);
        this.start = 0;
        this.stop = 0;
        this.traceInfo = null;
        this.timeline = null;
    }

    public MilliSpan(String description, String traceId, String spanId, Map<String, String> traceContext) {
        super(description, traceId, spanId);
        this.traceContext = traceContext;
        this.start = currentTimeMillis();
        this.stop = 0;
        addKVAnnotation(Constants.BAT_TRACE_TNAME, Utils.getTName());
        addKVAnnotation(Constants.BAT_TRACE_LOCAL_HOSTADDRESS, NetUtil.getLocalAddress().getHostAddress());
        addKVAnnotation(Constants.BAT_TRACE_LOCAL_HOSTNAME, NetUtil.getLocalAddress().getHostName());
    }


    private String generateNextChildSpanId() {
        return this.spanId + "." + index.incrementAndGet();
    }

    @Override
    public Span child(String childDescription) {
        return new MilliSpan(childDescription, traceId, generateNextChildSpanId());
    }

    @Override
    public String getChildNextId() {
        return generateNextChildSpanId();
    }

    @Override
    public synchronized void stop() {
        stop = System.currentTimeMillis();
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public synchronized boolean isRunning() {
        return start != 0 && stop == 0;
    }

    @Override
    public synchronized long getAccumulatedMillis() {
        if (start == 0)
            return 0;
        if (stop > 0)
            return stop - start;
        return currentTimeMillis() - start;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getSpanId() {
        return spanId;
    }

    @Override
    public long getStartTimeMillis() {
        return start;
    }

    @Override
    public long getStopTimeMillis() {
        return stop;
    }

    @Override
    public void addKVAnnotation(String key, String value) {
        if (traceInfo == null)
            traceInfo = new HashMap<String, String>();
        traceInfo.put(key, value);
    }

    @Override
    public void addTimelineAnnotation(String msg) {
        if (timeline == null) {
            timeline = new ArrayList<TimelineAnnotation>();
        }
        timeline.add(new TimelineAnnotation(System.currentTimeMillis(), msg));
    }

    @Override
    public Map<String, String> getKVAnnotations() {
        if (traceInfo == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(traceInfo);
    }

    @Override
    public List<TimelineAnnotation> getTimelineAnnotations() {
        if (timeline == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(timeline);
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

    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
