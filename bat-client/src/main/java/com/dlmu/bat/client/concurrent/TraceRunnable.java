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
package com.dlmu.bat.client.concurrent;

import com.dlmu.bat.client.DTraceClient;
import com.dlmu.bat.client.DTraceClientGetter;
import com.dlmu.bat.client.TraceScope;

import java.util.Map;

/**
 * Wrap a Runnable with a Span that survives a change in threads.
 */
public class TraceRunnable implements Runnable {

    private String description;
    private String traceId;
    private String spanId;
    private Map<String, String> traceContext;
    private Runnable runnable;

    public TraceRunnable(String traceId, String spanId, Map<String, String> traceContext,
                         Runnable runnable) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceContext = traceContext;
        this.runnable = runnable;
    }

    public TraceRunnable(String description, String traceId, String spanId, Map<String, String> traceContext,
                         Runnable runnable) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceContext = traceContext;
        this.runnable = runnable;
        this.description = description;
    }

    @Override
    public void run() {
        if (traceId == null) {
            runnable.run();
        } else {
            String description = this.description;
            if (description == null) {
                description = Thread.currentThread().getName();
            }
            DTraceClient dTraceClient = DTraceClientGetter.getClient();
            TraceScope traceScope = dTraceClient.newScope(description, traceId, spanId, traceContext);
            try {
                runnable.run();
            } finally {
                traceScope.close();
            }
        }
    }

    public Runnable getRunnable() {
        return runnable;
    }
}
