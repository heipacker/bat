package com.dlmu.bat.store.impl;

import com.dlmu.bat.common.BaseSpan;

import java.io.IOException;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class HBaseBaseSpan extends BaseSpan {

    public HBaseBaseSpan(byte[] sources) throws IOException {
        super(sources);
    }

    public byte[] toRowKey() {

        return null;
    }
}
