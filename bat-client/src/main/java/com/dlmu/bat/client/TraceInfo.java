package com.dlmu.bat.client;

import java.util.Collections;
import java.util.Map;

/**
 * @author heipacker on 16-5-21.
 */
public class TraceInfo {
    public final String traceId;
    public final String spanId;
    public final Map<String, String> traceContext;

    public TraceInfo(String traceId, String spanId) {
        this(traceId, spanId, Collections.<String, String>emptyMap());
    }

    public TraceInfo(String traceId, String spanId, Map<String, String> traceContext) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceContext = traceContext;
    }

    @Override
    public String toString() {
        return "TraceInfo(traceId=" + traceId + ", spanId=" + spanId + ")";
    }

}
