package com.dlmu.bat.store.impl;

import com.dlmu.bat.common.TraceIdWrapper;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class TraceIdUtils {

    public static String rowKey(String traceId) {
        return TraceIdWrapper.parseTraceId(traceId).rowKey();
    }

}
