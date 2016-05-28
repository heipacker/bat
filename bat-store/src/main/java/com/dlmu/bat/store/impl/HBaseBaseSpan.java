package com.dlmu.bat.store.impl;

import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.TraceIdWrapper;

import java.io.IOException;

/**
 * 添加rowkey方法
 * qualifer
 *
 * @author heipacker
 * @date 16-5-28.
 */
public class HBaseBaseSpan extends BaseSpan {

    private byte[] sources;

    public HBaseBaseSpan(byte[] sources) throws IOException {
        super(sources);
        this.sources = sources;
    }

    public String rowKey() {
        return TraceIdWrapper.parseTraceId(traceId).rowKey();
    }

    public String qualifier() {
        return spanId;
    }

    public byte[] toBytes() {
        return sources;
    }
}
