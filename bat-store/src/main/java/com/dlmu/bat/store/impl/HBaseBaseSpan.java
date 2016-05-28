package com.dlmu.bat.store.impl;

import com.dlmu.bat.common.BaseSpan;
import com.google.common.base.Charsets;

import java.io.IOException;

/**
 * 添加rowkey方法
 * qualifer
 * @author heipacker
 * @date 16-5-28.
 */
public class HBaseBaseSpan extends BaseSpan {

    public HBaseBaseSpan(byte[] sources) throws IOException {
        super(sources);
    }

    public String rowKey() {
        return TraceIdUtils.rowKey(traceId);
    }
    
    public String qualifier() {
        return spanId;
    }

    public byte[] toBytes() {
        return new byte[0];
    }
}
