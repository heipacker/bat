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

import com.google.common.collect.ImmutableMap;

import java.io.Closeable;

/**
 * Create a new DefaultTraceScope at major transitions. Hosts current tracing context.
 */
public class DefaultTraceScope implements TraceScope, Closeable {
    /**
     * The batClient to use for this scope.
     */
    final BatClient batClient;

    /**
     * The parentSpan of this trace scope, or null if there is no parentSpan.
     */
    private Span parentSpan;

    /**
     * The trace span for this scope, or null if the scope is closed.
     * <p>
     * If the scope is closed, it must also be detached.
     */
    private final Span span;

    /**
     * True if this scope is detached.
     */
    boolean detached;

    public DefaultTraceScope(BatClient batClient, Span span, Span parentSpan) {
        this.batClient = batClient;
        this.span = span;
        this.parentSpan = parentSpan;
        this.detached = false;
    }

    /**
     * Returns the span which this scope is managing.
     *
     * @return The span.
     */
    public Span getSpan() {
        return span;
    }

    /**
     * Returns the span ID which this scope is managing.
     *
     * @return The span ID.
     */
    public String getSpanId() {
        return span.getSpanId();
    }

    public Span getParentSpan() {
        return parentSpan;
    }

    public void setParentSpan(Span parentSpan) {
        this.parentSpan = parentSpan;
    }

    /**
     * Detach this DefaultTraceScope from the current thread.
     * <p>
     * It is OK to "leak" TraceScopes which have been detached.  They will not
     * consume any resources other than a small amount of memory until they are
     * garbage collected.  On the other hand, trace scopes which are still
     * attached must never be leaked.
     */
    public void detach() {
        if (detached) {
            DefaultBatClient.throwClientError("Can't detach this DefaultTraceScope  because " +
                    "it is already detached.");
        }
        DefaultBatClient.detachScope(this);
        detached = true;
        parentSpan = null;
    }

    /**
     * Attach this DefaultTraceScope to the current thread.
     */
    public void reattach() {
        if (!detached) {
            DefaultBatClient.throwClientError("Can't reattach this DefaultTraceScope  because " +
                    "it is not detached.");
        }
        DefaultBatClient.reattachScope(this);
        detached = false;
    }

    /**
     * Close this DefaultTraceScope, ending the trace span it is managing.
     */
    @Override
    public void close() {
        batClient.closeScope(this);
    }

    /**
     * @param key String key
     * @param value String value
     */
    public void addKVAnnotation(String key, String value) {
        span.addKVAnnotation(key, value);
    }

    /**
     * @param msg
     */
    public void addTimelineAnnotation(String msg) {
        span.addTimelineAnnotation(msg);
    }

    @Override
    public void addTraceContext(String key, String value) {
        span.addTraceContext(key, value);
    }

    @Override
    public ImmutableMap<String, String> getTraceContext() {
        return span.getTraceContext();
    }

}
